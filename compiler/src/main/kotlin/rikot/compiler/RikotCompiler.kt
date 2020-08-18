package rikot.compiler

import arrow.core.Left
import arrow.core.Right
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import rikot.compiler.text.TextInterpolator
import rikot.compiler.utility.escapeIfNecessary
import rikot.compiler.utility.readTextDetectingBom
import rikot.runtime.Text
import rikot.runtime.plus
import rikot.utility.fold
import rikot.utility.partitionMap
import rikot.utility.toList
import rikot.utility.zipAll
import java.io.File
import java.util.*
import kotlin.LazyThreadSafetyMode.PUBLICATION

private
fun MutableMap<String, String>.mergeVariable(variableName: String, variableType: String) =
    merge(variableName, variableType) { a, b ->
      when (a) {
        b -> a
        else -> throw IllegalArgumentException(
            "Variable '$variableName' has ambiguous types '$a' and '$b'"
        )
      }
    }

// Remove this if KotlinPoet add support to type variable escaping
private
fun TypeVariableName.Companion.escapeIfNecessary(variableName: String) =
    TypeVariableName(variableName.escapeIfNecessary())


private
val defaultFormats = sequenceOf("txt" to TextInterpolator)

class RikotCompiler private constructor(private val formats: Map<String, Interpolator>) {
  constructor(vararg formats: Pair<String, Interpolator>) : this((defaultFormats + formats).toMap())

  fun generateTypeSpec(packageName: String, outerClassName: String?, extension: String, source: File): TypeSpec =
      (formats[extension] ?: throw IllegalArgumentException("Unsupported format '$extension'")).let { interpolator ->
        source
            .readTextDetectingBom()
            .let { RikotParser(it) }
            .asCached()
            .partitionMap {
              when (it) {
                is Node.PlaceholderVariable -> Left(it)
                is ExpressionNode -> Right(it)
              }
            }
            .let { (placeholders, expressions) ->
              // Collect type information
              // TODO: Support polymorphic AST
              val defaultTargetType by lazy(PUBLICATION) {
                interpolator.defaultTargetType ?: throw IllegalArgumentException(
                    "'$extension' format does not support default target type in placeholder variables"
                )
              }
              val triples = interpolator(expressions).asCached()

              val (types: Map<String, String>, targetTypes: Map<String, TypeName>) = expressions
                  .mapNotNull { it as? ExpressionNode.Variable }
                  .zipAll(triples.mapNotNull { it.second }, null, null) { variable, target ->
                    checkNotNull(variable) { "Excessive target type emitted from the interpolator" }
                    checkNotNull(target) { "Insufficient target type emitted from the interpolator" }
                    variable to target
                  }
                  .fold(
                      TreeMap<String, String>() to TreeMap<String, TypeName>()
                  ) { (names, types), (variable, target) ->
                    names.mergeVariable(variable.name, variable.type)
                    types.merge(variable.type, target) { a, b ->
                      interpolator.resolveTargetType(variable.type, a, b)
                    }
                    names to types
                  }
                  .let { maps ->
                    placeholders
                        .fold(maps) { (names, types), variable ->
                          names.mergeVariable(variable.name, variable.type)
                          types.getOrPut(variable.type) { defaultTargetType }
                          names to types
                        }
                  }

              // Compose Kotlin source code
              val statement = triples
                  .map { it.first.first }
                  .fold(Text("return ")) { a, b -> a + b }
              val statementArguments = triples
                  .mapNotNull { it.first.second?.value }

              val typeVariableNames = targetTypes.keys.map { TypeVariableName.escapeIfNecessary(it) }
              val formatterInvoke = FunSpec
                  .builder("invoke")
                  .addModifiers(OPERATOR)
                  .addParameters(targetTypes.map { (type, targetType) ->
                    ParameterSpec(type, LambdaTypeName.get(
                        parameters = listOf(ParameterSpec.unnamed(TypeVariableName.escapeIfNecessary(type))),
                        returnType = targetType
                    ))
                  })
                  .build()

              TypeSpec
                  .objectBuilder(extension)
                  .addType(TypeSpec
                      .interfaceBuilder("Formatter")
                      .addTypeVariables(typeVariableNames)
                      .addFunction(formatterInvoke
                          .toBuilder()
                          .addModifiers(ABSTRACT)
                          .returns(interpolator.outputType)
                          .build())
                      .build())
                  .addFunction(FunSpec
                      .builder("invoke")
                      .addModifiers(OPERATOR)
                      .addTypeVariables(typeVariableNames)
                      .addParameters(types.map {
                        ParameterSpec(it.key, TypeVariableName.escapeIfNecessary(it.value))
                      })
                      .addStatement("return %L", TypeSpec
                          .anonymousClassBuilder()
                          .addSuperinterface(ParameterizedTypeName.run {
                            val className = when (outerClassName) {
                              null -> ClassName(packageName, extension, "Formatter")
                              else -> ClassName(packageName, outerClassName, extension, "Formatter")
                            }
                            className.parameterizedBy(typeVariableNames)
                          })
                          .addFunction(formatterInvoke
                              .toBuilder()
                              .addModifiers(OVERRIDE)
                              .addStatement(statement.compile(), *statementArguments.toList().toTypedArray())
                              .build())
                          .build())
                      .build())
                  .build()
            }
      }
}

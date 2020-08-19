package rikot.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

fun RikotCompiler.generateAll(sourceRoot: File, destination: File) {
  destination.deleteRecursively()

  sourceRoot
      .walkTopDown()
      .mapNotNull { f ->
        f.listFiles()?.let { f to it }
      }
      .flatMap { (directory, files) ->
        files
            .asSequence()
            .filter { it.isFile && it.extension == "rikot" }
            .map { source ->
              File(source.nameWithoutExtension).let { f ->
                when (val extension = f.extension) {
                  "" -> f.name to null
                  else -> f.nameWithoutExtension to extension
                }
              }
            }
            .groupBy({ it.first }) { it.second }
            .asSequence()
            .map { directory to it }
      }
      .forEach { (directory, sources) ->
        val sorted = sources.value.asSequence().filterNotNull().toSortedSet()
        val packageName = directory.toRelativeString(sourceRoot).replace(File.separatorChar, '.')
        val outer = sources.key

        val baseType = when {
          sorted.size < sources.value.size ->
            generateTypeSpec(packageName, null, outer, directory.resolve("$outer.rikot"))
                .toBuilder()
          else ->
            TypeSpec.objectBuilder(outer)
        }
        FileSpec
            .builder(packageName, outer)
            .addType(baseType
                .addTypes(sorted.map {
                  generateTypeSpec(packageName, outer, it, directory.resolve("$outer.$it.rikot"))
                })
                .build())
            .build()
            .writeTo(destination)
      }

  // Temporary workaround
  destination
      .walkTopDown()
      .filter { it.isFile }
      .forEach {
        it.writeText(it.readText().unescapeSpaces())
      }
}

package rikot.compiler

import com.squareup.kotlinpoet.TypeName

sealed class Node

sealed class PlaceholderNode : Node() {
  data class PlaceholderVariable(
      val name: String,
      val type: String
  ) : PlaceholderNode()

  data class PlaceholderVariableType(
      val type: String
  ) : PlaceholderNode()
}

sealed class ExpressionNode : Node() {
  data class Literal(
      val value: String
  ) : ExpressionNode()

  data class InlineLiteral(
      val value: String
  ) : ExpressionNode()

  data class Variable(
      val name: String,
      val type: String
  ) : ExpressionNode()
}


// Temporary workaround to prevent KotlinPoet from inserting line breaks inside variable names
private
fun String.escapeSpaces(): String =
    replace("_", "__").replace(" ", "_-")

internal
fun Node.escapeSpaces(): Node =
    when (this) {
      is ExpressionNode.Literal -> ExpressionNode.Literal(value.escapeSpaces())
      is ExpressionNode.InlineLiteral -> ExpressionNode.Literal(value.escapeSpaces())
      is ExpressionNode.Variable -> ExpressionNode.Variable(name.escapeSpaces(), type.escapeSpaces())
      is PlaceholderNode.PlaceholderVariable -> PlaceholderNode.PlaceholderVariable(name.escapeSpaces(), type.escapeSpaces())
      is PlaceholderNode.PlaceholderVariableType -> PlaceholderNode.PlaceholderVariableType(type.escapeSpaces())
    }

internal
fun String.unescapeSpaces(): String =
    replace("_-", " ").replace("__", "_")

internal
fun TypeName.unescapeSpaces(): String =
    toString().unescapeSpaces()

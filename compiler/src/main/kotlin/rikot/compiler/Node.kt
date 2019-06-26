package rikot.compiler

sealed class Node {
  data class PlaceholderVariable(
      val name: String,
      val type: String
  ) : Node()
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

@file:Suppress("ObjectPropertyName", "SpellCheckingInspection", "NonAsciiCharacters")
package rikot.compiler.text

import com.squareup.kotlinpoet.KOperator.PLUS
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import rikot.compiler.ExpressionNode
import rikot.compiler.Interpolator
import rikot.compiler.Interpolator.Argument
import rikot.compiler.Interpolator.Argument.OfMember
import rikot.compiler.Interpolator.Argument.OfString
import rikot.compiler.text.TextInterpolator.outputType
import rikot.compiler.utility.Pair3
import rikot.compiler.utility.className
import rikot.runtime.Text
import rikot.utility.Seq
import rikot.utility.plus
import rikot.utility.seqOf
import rikot.compiler.text.TextInterpolator as Runtime

private val `%N` by Text
private val `(%N)` by Text
private val `%M` by Text
private val ` %M (` by Text
private val `(%S)` by Text

object TextInterpolator : Interpolator {
  internal val Text = OfMember(MemberName("rikot.runtime", "Text"))
  internal val `+` = OfMember(MemberName("rikot.runtime", PLUS))

  override val outputType = className<Text>()
  override val defaultTargetType get() = outputType

  override
  operator fun invoke(nodes: Seq<ExpressionNode>): Seq<Pair3<Text, Argument?, TypeName?>> =
      when (nodes) {
        Seq.Nil -> Seq.Nil
        is Seq.Cons -> interpret(nodes, 0)
      }
}

private
fun interpret(nodes: Seq.Cons<ExpressionNode>, nesting: Int): Seq<Pair3<Text, Argument?, TypeName?>> =
    nodes.let { (head, tail) ->
      val suffix = {
        when (val t = tail()) {
          Seq.Nil -> seqOf(Text(nesting) { ')' } to null to null)
          is Seq.Cons -> (` %M (` to Runtime.`+` to null) + { interpret(t, 1 + nesting) }
        }
      }
      when (head) {
        is ExpressionNode.Literal ->
          (`%M` to Runtime.Text to null) + { (`(%S)` to OfString(head.value) to null) + suffix }
        is ExpressionNode.InlineLiteral ->
          (`%M` to Runtime.Text to null) + { (`(%S)` to OfString(head.value) to null) + suffix }
        is ExpressionNode.Variable ->
          (`%N` to OfString(head.type) to outputType) + { (`(%N)` to OfString(head.name) to null) + suffix }
      }
    }

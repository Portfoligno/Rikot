package rikot.compiler

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import rikot.compiler.utility.Pair3
import rikot.runtime.Text
import rikot.utility.Seq

interface Interpolator {
  val outputType: TypeName
  val defaultTargetType: TypeName?

  operator fun invoke(nodes: Seq<ExpressionNode>): Seq<Pair3<Text, Argument?, TypeName?>>

  sealed class Argument {
    abstract val value: Any

    data class OfString(override val value: String) : Argument()
    data class OfMember(override val value: MemberName) : Argument()
  }
}

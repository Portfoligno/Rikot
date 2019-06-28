package rikot.runtime

import rikot.runtime.utility.LiteralProperty
import rikot.utility.forEach

sealed class Text {
  abstract val length: Int

  abstract fun compile(): String

  override
  fun hashCode(): Int =
      strings().charHash(0)

  override
  fun equals(other: Any?): Boolean =
      other is Text && length == other.length && strings().charsMatch(other.strings(), 0)

  override
  fun toString(): String =
      compile()


  internal class Plain(val value: String) : Text() {
    override
    val length: Int
      get() = value.length

    override
    fun compile(): String =
        value
  }

  internal class Catenated(val prefix: Text, val suffix: Text) : Text() {
    override
    val length = prefix.length + suffix.length

    override
    fun compile(): String =
        StringBuilder(length).let { b ->
          strings().forEach { b.append(it) }
          b.toString()
        }
  }

  companion object : LiteralProperty<Text> {
    override
    fun getValue(propertyName: String): Text  =
        Plain(propertyName)

    val empty: Text =
        Plain("")
  }
}

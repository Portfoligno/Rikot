package rikot.compiler.utility

import rikot.runtime.utility.LiteralProperty
import rikot.utility.Seq
import rikot.utility.generateSeq
import java.lang.Character.*

inline class CodePoint(
    val value: Int
) {
  constructor(value: Char) : this(value.toInt())

  override
  fun toString() =
      when {
        isBmpCodePoint(value) ->
          value.toChar().toString()
        else ->
          String(charArrayOf(highSurrogate(value), lowSurrogate(value)))
      }

  companion object : LiteralProperty<CodePoint> {
    override
    fun getValue(propertyName: String): CodePoint =
        propertyName.codePointAndNextOffsetAt(0).let {
          when (it?.index) {
            propertyName.length -> it.value
            else -> throw IllegalArgumentException(propertyName)
          }
        }
  }
}


private
fun String.codePointAndNextOffsetAt(i1: Int): IndexedValue<CodePoint>? {
  if (i1 < length) {
    val c1 = this[i1]
    val i2 = 1 + i1

    if (c1.isHighSurrogate() && i2 < length) {
      val c2 = this[i2]

      if (c2.isLowSurrogate()) {
        return IndexedValue(1 + i2, CodePoint(toCodePoint(c1, c2)))
      }
    }
    return IndexedValue(i2, CodePoint(c1.toInt()))
  }
  return null
}

val String.codePointsWithEndIndexes: Seq<IndexedValue<CodePoint>>
  get() = generateSeq(codePointAndNextOffsetAt(0)) { (i) ->
    codePointAndNextOffsetAt(i)
  }


fun CodePoint.isLetterOrDigit() =
    isLetterOrDigit(value)

@file:Suppress("FunctionName")
package rikot.runtime

import rikot.utility.Seq
import rikot.utility.Stack
import rikot.utility.generateSeq

fun Text(value: String): Text =
    Text.Plain(value)

fun Text(size: Int, init: (Int) -> Char): Text =
    when (size) {
      0 -> Text.empty
      1 -> Text.Plain(init(0).toString())
      else -> Text.Plain(String(CharArray(size, init)))
    }

operator fun Text.plus(other: Text): Text =
    Text.Catenated(this, other)


private
tailrec infix fun Text.uncons(stack: Stack<Text>): Pair<String, Stack<Text>> =
    when (this) {
      is Text.Plain -> value to stack
      is Text.Catenated -> prefix uncons Stack.Cons(suffix, stack)
    }

internal
fun Text.strings(stack: Stack<Text> = Stack.Nil): Seq.Cons<String> =
    generateSeq(this uncons stack) { (_, q) ->
      when (q) {
        Stack.Nil -> null
        is Stack.Cons -> q.head uncons q.tail
      }
    }
        .map { it.first }


internal
tailrec fun Seq.Cons<String>.charsMatch(that: Seq.Cons<String>, offset: Int): Boolean {
  val thatHead = that.head
  val thatLength = thatHead.length - offset

  val thisHead = head
  val thisLength = thisHead.length
  val delta = thisLength - thatLength

  return when {
    delta == 0 -> when (val t1 = tail()) {
      Seq.Nil -> true
      is Seq.Cons -> when (val t2 = that.tail()) {
        Seq.Nil -> true
        is Seq.Cons -> t1.charsMatch(t2, 0)
      }
    }
    // `&&` does not work with tailrec?
    delta < 0 -> when {
      thisHead.regionMatches(0, thatHead, offset, thisLength) -> when (val t = tail()) {
        Seq.Nil -> true
        is Seq.Cons -> t.charsMatch(that, thisLength + offset)
      }
      else -> false
    }
    else -> when {
      thisHead.regionMatches(0, thatHead, offset, thatLength) -> when (val t = that.tail()) {
        Seq.Nil -> true
        is Seq.Cons -> t.charsMatch(this, thatLength)
      }
      else -> false
    }
  }
}


private
tailrec fun String.hash(offset: Int, h: Int): Int =
    if (offset < length) {
      hash(1 + offset, 31 * h + get(offset).toInt())
    } else {
      h
    }

internal
tailrec fun Seq.Cons<String>.charHash(h: Int): Int {
  val h1 = head.hash(0, h)

  return when (val t = tail()) {
    Seq.Nil -> h1
    is Seq.Cons -> t.charHash(h1)
  }
}

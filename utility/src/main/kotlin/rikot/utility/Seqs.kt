@file:Suppress("FunctionName")
package rikot.utility

private
fun <T> Array<T>.asSeq(index: Int): Seq<T> =
    if (index >= size) {
      Seq.Nil
    } else {
      Seq.Cons(this[index]) {
        asSeq(1 + index)
      }
    }


fun <T> seqOf(): Seq<T> =
    Seq.Nil

fun <T> seqOf(head: T): Seq<T> =
    Seq.Cons(head, ::seqOf)

fun <T> seqOf(head: T, vararg tail: T): Seq<T> =
    Seq.Cons(head) {
      tail.asSeq(0)
    }

operator fun <T> T.plus(tail: () -> Seq<T>): Seq<T> =
    Seq.Cons(this, tail)

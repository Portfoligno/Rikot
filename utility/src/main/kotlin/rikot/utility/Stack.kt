package rikot.utility

sealed class Stack<out T> {
  object Nil : Stack<Nothing>()

  data class Cons<out T>(val head: T, val tail: Stack<T>) : Stack<T>()
}

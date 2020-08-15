package rikot.compiler.utility

import rikot.utility.Stack

typealias Nat = Stack<Unit>

val Stack.Companion.Zero: Nat
  get() = Stack.Nil


val Nat.next: Nat
  get() = Stack.Cons(Unit, this)

val Nat.previous: Nat?
  get() = when (this) {
    Stack.Nil -> null
    is Stack.Cons -> tail
  }

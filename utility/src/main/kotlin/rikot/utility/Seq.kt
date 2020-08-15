package rikot.utility

sealed class Seq<out T> {
  abstract fun <R> map(transform: (T) -> R): Seq<R>


  object Nil : Seq<Nothing>() {
    override
    fun <R> map(transform: (Nothing) -> R): Nil =
        this
  }

  data class Cons<out T>(val head: T, val tail: () -> Seq<T>) : Seq<T>() {
    override
    fun <R> map(transform: (T) -> R): Cons<R> =
        Cons(transform(head)) {
          tail().map(transform)
        }
  }
}


fun <T : Any> generateSeq(seed: T?, nextFunction: (T) -> T?): Seq<T> =
    when (seed) {
      null -> Seq.Nil
      else -> generateSeq(seed, nextFunction)
    }

fun <T : Any> generateSeq(seed: T, nextFunction: (T) -> T?): Seq.Cons<T> =
    Seq.Cons(seed) {
      generateSeq(nextFunction(seed), nextFunction)
    }

tailrec fun <T> Seq<T>.forEach(action: (T) -> Unit): Unit =
    when (this) {
      Seq.Nil -> Unit
      is Seq.Cons -> {
        action(head)
        tail().forEach(action)
      }
    }

tailrec fun <T, R> Seq<T>.fold(initial: R, operation: (R, T) -> R): R =
    when (this) {
      Seq.Nil -> initial
      is Seq.Cons -> tail().fold(operation(initial, head), operation)
    }

fun <T, R> Seq<T>.foldRight(initial: R, operation: (T, () -> R) -> R): R =
    when (this) {
      Seq.Nil -> initial
      is Seq.Cons -> operation(head) {
        tail().foldRight(initial, operation)
      }
    }

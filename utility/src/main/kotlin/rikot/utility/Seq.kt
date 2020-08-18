package rikot.utility

import arrow.core.Either
import kotlin.LazyThreadSafetyMode.PUBLICATION

sealed class Seq<out T> {
  abstract fun <R> map(transform: (T) -> R): Seq<R>

  abstract fun <R> mapNotNull(transform: (T) -> R?): Seq<R>

  abstract fun filter(predicate: (T) -> Boolean): Seq<T>

  abstract fun filterNot(predicate: (T) -> Boolean): Seq<T>

  abstract fun asCached(mode: LazyThreadSafetyMode = PUBLICATION): Seq<T>


  object Nil : Seq<Nothing>() {
    override
    fun <R> map(transform: (Nothing) -> R): Nil =
        this

    override
    fun <R> mapNotNull(transform: (Nothing) -> R?): Nil =
        this

    override
    fun filter(predicate: (Nothing) -> Boolean): Nil =
        this

    override
    fun filterNot(predicate: (Nothing) -> Boolean): Nil =
        this

    override
    fun asCached(mode: LazyThreadSafetyMode): Nil =
        this
  }

  data class Cons<out T>(val head: T, val tail: () -> Seq<T>) : Seq<T>() {
    override
    fun <R> map(transform: (T) -> R): Cons<R> =
        Cons(transform(head)) {
          tail().map(transform)
        }

    override
    fun <R> mapNotNull(transform: (T) -> R?): Seq<R> =
        kotlin.run {
          tailrec fun <T> Cons<T>.go(transform: (T) -> R?): Seq<R> =
              when (val h = transform(head)) {
                null -> when (val t = tail()) {
                  Nil -> Nil
                  is Cons -> t.go(transform)
                }
                else -> Cons(h) {
                  tail().mapNotNull(transform)
                }
              }

          go(transform)
        }

    override
    fun filter(predicate: (T) -> Boolean): Seq<T> =
        kotlin.run {
          tailrec fun <T> Cons<T>.go(predicate: (T) -> Boolean): Seq<T> =
              when {
                predicate(head) -> Cons(head) {
                  tail().filter(predicate)
                }
                else -> when (val t = tail()) {
                  Nil -> Nil
                  is Cons -> t.go(predicate)
                }
              }

          go(predicate)
        }

    override
    fun filterNot(predicate: (T) -> Boolean): Seq<T> =
        filter {
          !predicate(it)
        }

    override
    fun asCached(mode: LazyThreadSafetyMode): Seq<T> =
        Cons(head, lazy(mode) { tail().asCached() }::value)
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


/**
 * Split this [Seq] into a pair of [Seq]s by applying the supplied function.
 * [Seq.asCached] could be used together to avoid repeated processing.
 */
fun <T, R1, R2> Seq<T>.partitionMap(transform: (T) -> Either<R1, R2>): Pair<Seq<R1>, Seq<R2>> =
    map(transform).let { mapped ->
      mapped.mapNotNull { (it as? Either.Left)?.a } to mapped.mapNotNull { (it as? Either.Right)?.b }
    }

fun <T, R, V> Seq<T>.zipAll(other: Seq<R>, thisPadding: T, otherPadding: R, transform: (T, R) -> V): Seq<V> =
    when (this) {
      Seq.Nil -> other.map { transform(thisPadding, it) }
      is Seq.Cons -> when (other) {
        Seq.Nil -> map { transform(it, otherPadding) }
        is Seq.Cons -> Seq.Cons(transform(head, other.head)) {
          tail().zipAll(other.tail(), thisPadding, otherPadding, transform)
        }
      }
    }

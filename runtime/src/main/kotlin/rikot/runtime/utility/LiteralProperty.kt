package rikot.runtime.utility

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface LiteralProperty<T> : ReadOnlyProperty<Any?, T> {
  fun getValue(propertyName: String): T

  override
  fun getValue(thisRef: Any?, property: KProperty<*>): T =
      getValue(property.name)

  fun replacing(vararg pairs: Pair<Char, Char>): ReadOnlyProperty<Any?, T> =
      object : ReadOnlyProperty<Any?, T> {
        override
        fun getValue(thisRef: Any?, property: KProperty<*>): T =
            getValue(pairs.fold(property.name) { s, (a, b) ->
              s.replace(a, b)
            })
      }
}

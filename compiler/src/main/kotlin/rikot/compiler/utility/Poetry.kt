package rikot.compiler.utility

import com.squareup.kotlinpoet.asClassName

inline fun <reified T> className() =
    T::class.asClassName()

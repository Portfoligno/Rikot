package rikot.compiler.utility

import com.squareup.kotlinpoet.asClassName

private
val escapeIfNecessary = Class
    .forName("com.squareup.kotlinpoet.UtilKt")
    .getDeclaredMethod("escapeIfNecessary", String::class.java, Boolean::class.java)

inline fun <reified T> className() =
    T::class.asClassName()

internal
fun String.escapeIfNecessary(): String =
    escapeIfNecessary.invoke(null, this, false) as String

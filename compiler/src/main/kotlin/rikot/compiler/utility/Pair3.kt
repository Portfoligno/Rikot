package rikot.compiler.utility

/**
 * Same as `Triple`, but allow you to say `a to b to c`
 */
typealias Pair3<A, B, C> = Pair<Pair<A, B>, C>

package com.virtuslab.pulumikotlin.codegen.utils

@Suppress("UnsafeCallOnNullableType")
fun <K, V> Map<K, V?>.filterNotNullValues() =
    this
        .mapNotNull { (key, value) ->
            value?.let { key to value }
        }
        .toMap()

fun <K, V> Grouping<K, V>.valuesToSet() =
    valuesToSet { it }

fun <K, V, R> Grouping<V, K>.valuesToSet(valueSelector: (V) -> R): Map<K, Set<R>> {
    return fold(emptySet()) { acc, v -> acc + valueSelector(v) }
}

package com.virtuslab.pulumikotlin.codegen.utils

import java.util.Locale

fun <T> T.letIf(predicate: (T) -> Boolean, mapper: (T) -> T) =
    if (predicate(this)) {
        mapper(this)
    } else {
        this
    }

fun <T> T.letIf(predicate: Boolean, mapper: (T) -> T) =
    letIf({ predicate }, mapper)

fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.decapitalize() =
    replaceFirstChar { it.lowercase(Locale.getDefault()) }

@Suppress("UnsafeCallOnNullableType")
fun <K, V> Map<K, V?>.filterNotNullValues() =
    this
        .filter { it.value != null }
        .map { (key, value) -> key to value!! }
        .toMap()

fun <K, V> Grouping<K, V>.valuesToSet() =
    valuesToSet { it }

fun <K, V, R> Grouping<V, K>.valuesToSet(valueSelector: (V) -> R): Map<K, Set<R>> {
    return fold(emptySet()) { acc, v -> acc + valueSelector(v) }
}

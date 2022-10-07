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

fun <K, V> Map<K, V?>.filterNotNullValues() =
    this
        .filter { it.value != null }
        .map { (key, value) -> key to value!! }
        .toMap()

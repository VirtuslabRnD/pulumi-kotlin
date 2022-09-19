package com.virtuslab.pulumikotlin.codegen.utils

import java.util.Locale

fun <T> T.letIf(what: (T) -> Boolean, mapper: (T) -> T): T {
    return if (what(this)) {
        mapper(this)
    } else {
        this
    }
}

fun <T> T.letIf(what: Boolean, mapper: (T) -> T): T {
    return if (what) {
        mapper(this)
    } else {
        this
    }
}

fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.decapitalize() =
    replaceFirstChar { it.lowercase(Locale.getDefault()) }

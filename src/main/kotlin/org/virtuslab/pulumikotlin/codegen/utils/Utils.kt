package org.virtuslab.pulumikotlin.codegen.utils

fun <T> T.letIf(predicate: (T) -> Boolean, mapper: (T) -> T) =
    if (predicate(this)) {
        mapper(this)
    } else {
        this
    }

fun <T> T.letIf(predicate: Boolean, mapper: (T) -> T) =
    letIf({ predicate }, mapper)

fun Long.isOdd() = this % 2L != 0L

fun Long.isEven() = !isOdd()

fun Int.isOdd() = toLong().isOdd()

fun Int.isEven() = !isOdd()

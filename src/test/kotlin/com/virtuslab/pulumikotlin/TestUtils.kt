package com.virtuslab.pulumikotlin

import com.pulumi.core.Output
import org.junit.jupiter.api.Assertions.assertEquals

internal fun <T, R> assertOutputPropertyEqual(
    objectWithProperty: T,
    expectedValue: R,
    errorMessagePrefix: String? = null,
    actualOutputValueExtractor: (T) -> Output<R>?,
): () -> Unit {
    val outputWithActualValue = actualOutputValueExtractor(objectWithProperty)
    return assertOutputEqual(outputWithActualValue, expectedValue, errorMessagePrefix)
}

internal fun <T> assertOutputEqual(
    outputWithActualValue: Output<T>?,
    expectedValue: T,
    errorMessagePrefix: String? = null,
): () -> Unit {
    val actualValue = extractOutputValue(outputWithActualValue)
    return { assertEquals(expectedValue, actualValue, errorMessagePrefix) }
}

internal fun <T> extractOutputValue(output: Output<T>?): T? {
    var value: T? = null
    output?.applyValue { value = it }
    return value
}

internal fun <T> concat(iterableOfIterables: Iterable<Iterable<T>>?): List<T> =
    iterableOfIterables?.flatten() ?: emptyList()

internal fun <T> concat(vararg iterables: Iterable<T>?): List<T> =
    concat(iterables.filterNotNull().asIterable())

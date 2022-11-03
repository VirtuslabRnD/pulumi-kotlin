package com.virtuslab.pulumikotlin

import com.pulumi.core.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiNamingConfiguration
import org.junit.jupiter.api.Assertions.assertFalse

internal fun <T> extractOutputValue(output: Output<T>?): T? {
    var value: T? = null
    output?.applyValue { value = it }
    return value
}

internal fun <T> concat(iterableOfIterables: Iterable<Iterable<T>>?): List<T> =
    iterableOfIterables?.flatten() ?: emptyList()

internal fun <T> concat(vararg iterables: Iterable<T>?): List<T> =
    concat(iterables.filterNotNull().asIterable())

internal fun namingConfigurationWithSlashInModuleFormat(providerName: String) =
    PulumiNamingConfiguration.create(providerName = providerName, moduleFormat = "(.*)(?:/[^/]*)")

private fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

internal fun <T> assertDoesNotContain(iterable: Iterable<T>, element: T, message: String? = null) {
    val prefix = messagePrefix(message)
    assertFalse(iterable.contains(element)) {
        """
            $prefix Expected the collection to not contain the element.
            Collection <$iterable>, element <$element>.
        """.trimIndent()
    }
}

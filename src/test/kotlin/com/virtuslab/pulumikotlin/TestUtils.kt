package com.virtuslab.pulumikotlin

import com.pulumi.core.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiNamingConfiguration

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
    PulumiNamingConfiguration(providerName = providerName, moduleFormat = "(.*)(?:/[^/]*)")

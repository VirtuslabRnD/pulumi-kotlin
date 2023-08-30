package com.pulumi.kotlin

import com.pulumi.core.Output

internal fun <T> extractOutputValue(output: Output<T>?): T? {
    var value: T? = null
    output?.applyValue { value = it }
    return value
}

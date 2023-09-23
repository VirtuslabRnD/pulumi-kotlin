@file:Suppress("unused")

package com.pulumi.kotlin

import com.pulumi.core.Output
import com.pulumi.core.internal.OutputData
import com.pulumi.core.internal.OutputInternal
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

object Output {

    /**
     * Use this method to open an [OutputInterpolationContext] in which the unary plus operator can be used on outputs 
     * to interpolate their asynchronous values into an asynchronous string wrapped in an output.
     * ```kotlin
     * val myOutput = Output.of("value")
     * val interpolatedString = interpolate { "The value of this output is: ${+myOutput}" }
     * ```
     * 
     * This method is an alternative to the [Output.format] method from Java.
     * ```java
     * Output<String> myOutput = Output.of("value");
     * Output<String> interpolatedString = String.format("The value of this output is: %s", myOutput);
     * ```
     * @param format a function returning a Kotlin-style interpolated string with [Output] instances marked with [OutputInterpolationContext.unaryPlus]
     * @return an asynchronous [Output]-wrapped string with Kotlin-style interpolated values
     */
    suspend fun interpolation(format: suspend OutputInterpolationContext.() -> String): Output<String> {
        val context = OutputInterpolationContext()
        return try {
            val output = Output.of(context.format())
            if (context.isSecret) {
                output.asSecret()
            } else {
                output
            }
        } catch (e: UnknownOutputError) {
            if (context.isSecret) {
                unknownOutput<String>().asSecret()
            } else {
                unknownOutput()
            }
        }
    }
}

/**
 *  The context for Kotlin-style [Output] interpolation.
 *
 * @property isSecret denotes whether any of the interpolated values contain a secret
 */
class OutputInterpolationContext internal constructor(var isSecret: Boolean = false)  {
    /**
     * The unary plus operator that can be used on [Output] instances within [OutputInterpolationContext].
     *
     * @return an asynchronous value of the [Output] in question converted to a string with [toString]
     */
    suspend operator fun <T> Output<T>.unaryPlus(): String {
        return interpolate()
    }

    // TODO: decide if we prefer this or the unary plus
    suspend fun <T> Output<T>.interpolate(): String {
        val outputData = (this as OutputInternal<T>)
            .dataAsync
            .await()

        val value = outputData.valueNullable?.toString()

        if (outputData.isSecret) {
            this@OutputInterpolationContext.isSecret = true
        }

        return value ?: throw UnknownOutputError()
    }
}

private class UnknownOutputError : RuntimeException("Cannot interpolate an output whose value is unknown")

private fun <T> unknownOutput(): Output<T> {
    return OutputInternal(CompletableFuture.completedFuture(OutputData.unknown()))
}

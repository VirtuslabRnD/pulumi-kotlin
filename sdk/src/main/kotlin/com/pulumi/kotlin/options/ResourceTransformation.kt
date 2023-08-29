package com.pulumi.kotlin.options

import com.pulumi.kotlin.PulumiNullFieldException
import java.util.Optional
import com.pulumi.resources.ResourceArgs as JavaResourceArgs
import com.pulumi.resources.ResourceOptions as JavaResourceOptions
import com.pulumi.resources.ResourceTransformation as JavaResourceTransformation
import com.pulumi.resources.ResourceTransformation.Args as JavaResourceTransformationArgs
import com.pulumi.resources.ResourceTransformation.Result as JavaResourceTransformationResult

/**
 * The callback signature for the [CustomResourceOptions.resourceTransformations] option.
 *
 * @see [JavaResourceTransformation]
 */
fun interface ResourceTransformation {
    fun apply(args: JavaResourceTransformationArgs): JavaResourceTransformationResult?
}

internal fun ResourceTransformation.toJava(): JavaResourceTransformation {
    return JavaResourceTransformation { javaArgs -> Optional.ofNullable(this.apply(javaArgs)) }
}

internal fun JavaResourceTransformation.toKotlin(): ResourceTransformation {
    return ResourceTransformation { this.apply(it).orElse(null) }
}

/**
 * Builder for [JavaResourceTransformationResult].
 */
class ResourceTransformationResultBuilder(
    var args: JavaResourceArgs? = null,
    var options: JavaResourceOptions? = null,
) {

    fun args(value: JavaResourceArgs) {
        this.args = value
    }

    fun options(value: JavaResourceOptions) {
        this.options = value
    }

    internal fun build(): JavaResourceTransformationResult = JavaResourceTransformationResult(
        args ?: throw PulumiNullFieldException("args"),
        options ?: throw PulumiNullFieldException("options"),
    )
}

/**
 * Creates [JavaResourceTransformationResult]
 * with use of type-safe [ResourceTransformationResultBuilder].
 */
fun transformationResult(
    block: ResourceTransformationResultBuilder.() -> Unit,
): JavaResourceTransformationResult {
    val builder = ResourceTransformationResultBuilder()
    block(builder)
    return builder.build()
}

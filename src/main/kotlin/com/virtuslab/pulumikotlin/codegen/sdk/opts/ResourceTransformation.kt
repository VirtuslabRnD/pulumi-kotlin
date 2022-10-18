// This class is included in the generated code. The package name matches its location in the generated code.
@file:Suppress("InvalidPackageDeclaration", "PackageDirectoryMismatch", "unused")

package com.pulumi.kotlin.options

import com.pulumi.kotlin.toKotlin
import java.util.Optional

/**
 * The callback signature for the [CustomResourceOptions.resourceTransformations] option.
 *
 * @see [com.pulumi.resources.ResourceTransformation]
 */
fun interface ResourceTransformation {
    fun apply(args: com.pulumi.resources.ResourceTransformation.Args):
        com.pulumi.resources.ResourceTransformation.Result?
}

internal fun ResourceTransformation.toJava(): com.pulumi.resources.ResourceTransformation {
    return com.pulumi.resources.ResourceTransformation { javaArgs -> Optional.ofNullable(this.apply(javaArgs)) }
}

internal fun com.pulumi.resources.ResourceTransformation.toKotlin(): ResourceTransformation {
    return ResourceTransformation { this.apply(it).toKotlin() }
}

class ResourceTransformationResultBuilder(
    var args: com.pulumi.resources.ResourceArgs? = null,
    var options: com.pulumi.resources.ResourceOptions? = null,
) {

    fun args(value: com.pulumi.resources.ResourceArgs) {
        this.args = value
    }

    fun options(value: com.pulumi.resources.ResourceOptions) {
        this.options = value
    }

    internal fun build(): com.pulumi.resources.ResourceTransformation.Result {
        return com.pulumi.resources.ResourceTransformation.Result(
            args!!,
            options!!,
        )
    }
}

/**
 * Creates [com.pulumi.resources.ResourceTransformation.Result]
 * with use of type-safe [ResourceTransformationResultBuilder].
 */
fun transformationResult(
    block: ResourceTransformationResultBuilder.() -> Unit,
): com.pulumi.resources.ResourceTransformation.Result {
    val builder = ResourceTransformationResultBuilder()
    block(builder)
    return builder.build()
}

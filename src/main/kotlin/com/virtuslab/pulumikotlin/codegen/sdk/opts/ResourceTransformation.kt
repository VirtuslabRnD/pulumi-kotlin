// This class is included in the generated code. The package name matches its location in the generated code.
@file:Suppress("InvalidPackageDeclaration", "PackageDirectoryMismatch")

package com.pulumi.kotlin.options

import com.pulumi.kotlin.toKotlin
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
    return ResourceTransformation { this.apply(it).toKotlin() }
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

    @Suppress("UnsafeCallOnNullableType") // NPE calls will be removed in the future https://github.com/VirtuslabRnD/pulumi-kotlin/issues/62
    internal fun build(): JavaResourceTransformationResult = JavaResourceTransformationResult(args!!, options!!)
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

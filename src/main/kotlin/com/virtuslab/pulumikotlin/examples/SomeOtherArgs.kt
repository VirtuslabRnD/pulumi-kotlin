package com.virtuslab.pulumikotlin.examples

import com.pulumi.core.Output

data class SomeOtherArgs(
    val someOtherNestedArgs: Output<SomeOtherNestedArgs>,
)

@PulumiTagMarker
data class SomeOtherArgsBuilder(
    var someOtherNestedArgs: Output<SomeOtherNestedArgs>? = null,
) {
    fun someOtherNestedArgs(args: SomeOtherNestedArgs) {
        this.someOtherNestedArgs = Output.of(args)
    }

    fun someOtherNestedArgs(block: SomeOtherNestedArgsBuilder.() -> Unit) {
        val builder = SomeOtherNestedArgsBuilder()
        builder.block()
        someOtherNestedArgs(builder.build())
    }

    fun build(): SomeOtherArgs {
        return SomeOtherArgs(someOtherNestedArgs!!)
    }
}

fun S3ArgsBuilder.someOtherArgs(block: SomeOtherArgsBuilder.() -> Unit) {
    val builder = SomeOtherArgsBuilder()
    builder.block()
    someOtherArgs(builder.build())
}

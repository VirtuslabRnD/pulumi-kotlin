package xyz.mf7.kotlinpoet.exampleapi

import com.pulumi.core.Output

data class SomeOtherArgs(
    val someOtherNestedArgs: Output<SomeOtherNestedArgs>
)


@PulumiTagMarker
data class SomeOtherArgsBuilder(
    var someOtherNestedArgs: Output<SomeOtherNestedArgs>? = null
) {
    fun someOtherNestedArgs(args: SomeOtherNestedArgs) {
        this.someOtherNestedArgs = Output.of(args)
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

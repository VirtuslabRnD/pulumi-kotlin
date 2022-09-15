package com.virtuslab.pulumikotlin.examples

import com.pulumi.core.Output

data class SomeOtherNestedArgs(
    val name: Output<String>,
    val number: Output<Int>,
)

@PulumiTagMarker
data class SomeOtherNestedArgsBuilder(
    private var name: Output<String>? = null,
    private var number: Output<Int>? = null,
) {
    fun name(name: String) {
        this.name = Output.of(name)
    }

    fun number(number: Int) {
        this.number = Output.of(number)
    }

    fun build(): SomeOtherNestedArgs {
        return SomeOtherNestedArgs(name!!, number!!)
    }
}

// fun someOtherNestedArgs(block: SomeOtherNestedArgsBuilder.() -> Unit): SomeOtherNestedArgs {
//    val builder = SomeOtherNestedArgsBuilder()
//    builder.block()
//    return builder.build()
// }

// fun SomeOtherArgsBuilder.someOtherNestedArgs(block: SomeOtherNestedArgsBuilder.() -> Unit) {
//    val builder = SomeOtherNestedArgsBuilder()
//    builder.block()
//    someOtherNestedArgs(builder.build())
// }

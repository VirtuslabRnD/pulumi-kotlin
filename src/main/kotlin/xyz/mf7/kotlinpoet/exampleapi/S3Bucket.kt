package xyz.mf7.kotlinpoet.exampleapi

import com.pulumi.aws.s3.BucketV2
import com.pulumi.aws.s3.BucketV2Args
import com.pulumi.core.Output
import com.pulumi.resources.CustomResourceOptions
import kotlinx.coroutines.runBlocking

data class S3Args(
    val hostedZoneId: Output<String>,
    val tags: Output<Map<String, String>>,
    val someOtherArgs: Output<SomeOtherArgs>
)

@DslMarker
annotation class PulumiTagMarker


@PulumiTagMarker
class S3ArgsBuilder {
    private var hostedZoneId: Output<String>? = null
    private var tags: Output<Map<String, String>>? = null
    private var someOtherArgs: Output<SomeOtherArgs>? = null
    private var someOtherArgs2: Output<List<SomeOtherArgs>>? = null

    fun hostedZoneId(value: String) {
        this.hostedZoneId = Output.of(value)
    }

    fun tags(value: Map<String, String>) {
        this.tags = Output.of(value)
    }

    fun hostedZoneId(value: Output<String>) {
        this.hostedZoneId = value
    }

    fun tags(value: Output<Map<String, String>>) {
        this.tags = value
    }

    fun tags(vararg value: Pair<String, String>) {
        this.tags = Output.of(value.toMap())
    }

    fun someOtherArgs(args: SomeOtherArgs) {
        this.someOtherArgs = Output.of(args)
    }

    fun someOtherArgs(args: SomeOtherArgsBuilder.() -> Unit) {
        this.someOtherArgs = Output.of(SomeOtherArgsBuilder().apply { args() }.build())
    }

    fun someOtherArgs2(vararg args: SomeOtherArgsBuilder.() -> Unit) {
        val built = args.map {method ->
            SomeOtherArgsBuilder().apply { method() }.build()
        }
        this.someOtherArgs2 = Output.of(built)
    }

    fun build(): S3Args {
        return S3Args(hostedZoneId!!, tags!!, someOtherArgs!!)
    }
}

data class S3Bucket(val name: String, val args: S3Args, val custom: CustomArgs)

@PulumiTagMarker
class S3BucketBuilder {

    var name: String? = null
    var args: S3Args? = null
    var custom: CustomArgs = CustomArgs()

    fun build(): S3Bucket {
        return S3Bucket(name!!, args!!, custom)
    }
}

suspend fun s3Bucket(name: String, block: suspend S3BucketBuilder.() -> Unit): S3Bucket {
    val builder = S3BucketBuilder()

    builder.name = name

    block(builder)

    return builder.build()
}

fun toJava(s3Bucket: S3Bucket): BucketV2 {
    return BucketV2(
        s3Bucket.name,
        BucketV2Args.builder()
            .hostedZoneId(s3Bucket.args.hostedZoneId)
            .tags(s3Bucket.args.tags)
            .build(),
        CustomResourceOptions.builder()
            .deleteBeforeReplace(s3Bucket.custom.deleteBeforeReplace)
            .protect(s3Bucket.custom.protect)
            .build()
    )
}


suspend fun S3BucketBuilder.args(block: suspend S3ArgsBuilder.() -> Unit): Unit {
    val builder = S3ArgsBuilder()

    block(builder)

    this.args = builder.build()
}

data class CustomArgs(
    val deleteBeforeReplace: Boolean = false,
    val protect: Boolean = false
)

@PulumiTagMarker
class CustomArgsBuilder {
    private var deleteBeforeReplace: Boolean = false
    private var protect: Boolean = false

    fun deleteBeforeReplace(value: Boolean) {
        deleteBeforeReplace = value
    }

    fun protect(value: Boolean) {
        protect = value
    }

    fun build(): CustomArgs = CustomArgs(
        deleteBeforeReplace,
        protect
    )
}

suspend fun S3BucketBuilder.custom(block: suspend CustomArgsBuilder.() -> Unit): Unit {
    val builder = CustomArgsBuilder()

    block(builder)

    this.custom = builder.build()
}

suspend fun args(block: suspend S3ArgsBuilder.() -> Unit): S3Args {
    val builder = S3ArgsBuilder()

    block(builder)

    return builder.build()
}



suspend fun createInfra() {

    val bucket = s3Bucket("bucket-name-here") {
        args {
            hostedZoneId("whatever")
            tags(
                "a" to "b"
            )
            someOtherArgs {
                someOtherNestedArgs {
                    name("whatever")
                    number(50)
                }
            }
            someOtherArgs2(
                {
                    someOtherNestedArgs {
                        name("whatever2")
                        number(50)
                    }
                },
                {
                    someOtherNestedArgs {
                        name("whatever3")
                        number(60)
                    }
                }
            )
        }

        custom {
            deleteBeforeReplace(true)
            protect(true)
        }
    }
}

fun main() {
    runBlocking {
        createInfra()
    }
}
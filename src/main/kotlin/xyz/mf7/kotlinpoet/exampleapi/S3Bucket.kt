package xyz.mf7.kotlinpoet.exampleapi

import com.pulumi.aws.s3.BucketV2
import com.pulumi.aws.s3.BucketV2Args
import com.pulumi.core.Output
import com.pulumi.kotlin.aws.emr.GetReleaseLabelsArgs
import com.pulumi.kotlin.aws.emr.GetReleaseLabelsFilters
import com.pulumi.kotlin.aws.emr.getReleaseLabels
import com.pulumi.resources.CustomResourceOptions
import kotlinx.coroutines.runBlocking

data class S3Args(
    val hostedZoneId: Output<String>,
    val tags: Output<Map<String, String>>
)

@DslMarker
annotation class PulumiTagMarker


@PulumiTagMarker
class S3ArgsBuilder {
    private var hostedZoneId: Output<String>? = null
    private var tags: Output<Map<String, String>>? = null

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

    fun build(): S3Args {
        return S3Args(hostedZoneId!!, tags!!)
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

    builder.build()
}

suspend fun createInfra() {

    val standaloneArgs = args {
        hostedZoneId("whatever")
        tags(
            mapOf("a" to "b")
        )
    }

    s3Bucket("bucket-name-here") {
        args = standaloneArgs

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
package com.pulumi.kotlin

import com.pulumi.Context
import com.pulumi.core.Either
import com.pulumi.core.Output
import com.pulumi.resources.CustomResourceOptions
import kotlinx.coroutines.runBlocking
import java.util.Optional

@DslMarker
annotation class PulumiTagMarker

data class Resource(
    val whatever: String,
)

data class ProviderResource(
    val whatever: String,
)

data class CustomTimeouts(
    val whatever: String,
)

data class ResourceTransformation(
    val whatever: String,
)

data class Alias(
    val whatever: String,
)

// should these defaults be set explicitly?
data class CustomArgs(
    val protect: Boolean = false,
    val id: Output<String>? = null,
    val parent: Resource? = null,
    val dependsOn: Output<List<Resource>>? = null,
    val ignoreChanges: List<String>? = null,
    val version: String? = null,
    val provider: ProviderResource? = null,
    val customTimeouts: CustomTimeouts? = null,
    val resourceTransformations: List<ResourceTransformation>? = null,
    val aliases: List<Output<Alias>>? = null,
    val urn: String? = null,
    val deleteBeforeReplace: Boolean = false,
    val additionalSecretOutputs: List<String>? = null,
    val importId: String? = null,
    val replaceOnChanges: List<String>? = null,
    val retainOnDelete: Boolean = false,
    val pluginDownloadURL: String? = null,
) {
    fun toJava(): CustomResourceOptions {
        return CustomResourceOptions.builder().build()
    }
}

data class CustomArgsBuilder(
    var protect: Boolean = false,
    var id: Output<String>? = null,
    var parent: Resource? = null,
    var dependsOn: Output<List<Resource>>? = null,
    var ignoreChanges: List<String>? = null,
    var version: String? = null,
    var provider: ProviderResource? = null,
    var customTimeouts: CustomTimeouts? = null,
    var resourceTransformations: List<ResourceTransformation>? = null,
    var aliases: List<Output<Alias>>? = null,
    var urn: String? = null,
    var deleteBeforeReplace: Boolean = false,
    var additionalSecretOutputs: List<String>? = null,
    var importId: String? = null,
    var replaceOnChanges: List<String>? = null,
    var retainOnDelete: Boolean = false,
    var pluginDownloadURL: String? = null,
) {
    fun protect(value: Boolean) {
        this.protect = value
    }

    fun id(value: Output<String>?) {
        this.id = value
    }

    fun parent(value: Resource?) {
        this.parent = value
    }

    fun dependsOn(value: Output<List<Resource>>?) {
        this.dependsOn = value
    }

    fun ignoreChanges(value: List<String>?) {
        this.ignoreChanges = value
    }

    fun version(value: String?) {
        this.version = value
    }

    fun provider(value: ProviderResource?) {
        this.provider = value
    }

    fun customTimeouts(value: CustomTimeouts?) {
        this.customTimeouts = value
    }

    fun resourceTransformations(value: List<ResourceTransformation>?) {
        this.resourceTransformations = value
    }

    fun aliases(value: List<Output<Alias>>?) {
        this.aliases = value
    }

    fun urn(value: String?) {
        this.urn = value
    }

    fun deleteBeforeReplace(value: Boolean) {
        this.deleteBeforeReplace = value
    }

    fun additionalSecretOutputs(value: List<String>?) {
        this.additionalSecretOutputs = value
    }

    fun importId(value: String?) {
        this.importId = value
    }

    fun replaceOnChanges(value: List<String>?) {
        this.replaceOnChanges = value
    }

    fun retainOnDelete(value: Boolean) {
        this.retainOnDelete = value
    }

    fun pluginDownloadURL(value: String?) = null

    fun build(): CustomArgs {
        return CustomArgs(
            protect,
            id,
            parent,
            dependsOn,
            ignoreChanges,
            version,
            provider,
            customTimeouts,
            resourceTransformations,
            aliases,
            urn,
            deleteBeforeReplace,
            additionalSecretOutputs,
            importId,
            replaceOnChanges,
            retainOnDelete,
            pluginDownloadURL,
        )
    }
}

// TODO: make sure these helpers do not leak to the SDK

fun <T> List<ConvertibleToJava<T>>.toJava(): List<T> {
    return map { it.toJava() }
}

fun <T, T2> Map<T, ConvertibleToJava<T2>>.toJava(): Map<T, T2> {
    return map { (key, value) -> key to value.toJava() }.toMap()
}

fun <T> Output<out ConvertibleToJava<T>>.toJava(): Output<T> {
    return applyValue { it.toJava() }
}

@JvmName("A0E1B7D29")
fun <T1, T2> Output<out Map<T1, ConvertibleToJava<T2>>>.toJava(): Output<Map<T1, T2>> {
    return applyValue { it.toJava() }
}

@JvmName("AB82F3249")
fun <T> Output<out List<ConvertibleToJava<T>>>.toJava(): Output<List<T>> {
    return applyValue { it.toJava() }
}

@JvmName("A500F3FFF")
fun <T> Output<T>.toJava(): Output<T> {
    return this
}

@JvmName("A1E842B23")
fun <T> List<T>.toJava(): List<T> {
    return this
}

@JvmName("A3F01FCF1")
fun <T1, T2> Map<T1, T2>.toJava(): Map<T1, T2> {
    return this
}

@JvmName("A3F01FCF2")
fun <T, R> Output<out Either<T, out ConvertibleToJava<R>>>.toJava(): Output<Either<T, R>> {
    return applyValue { either -> either.transform({ it }, { it.toJava() }) }
}

@JvmName("A3F01FCF3")
fun <T, R> Output<out Either<out ConvertibleToJava<T>, out ConvertibleToJava<R>>>.toJava(): Output<Either<T, R>> {
    return applyValue { either -> either.transform({ it.toJava() }, { it.toJava() }) }
}

@JvmName("A3F01FCF4")
fun <T, R> Output<out Either<out ConvertibleToJava<T>, R>>.toJava(): Output<Either<T, R>> {
    return applyValue { either -> either.transform({ it.toJava() }, { it }) }
}

fun <T> Optional<T>.toKotlin(): T? {
    return this.orElseGet { null }
}

fun <T> T.toKotlin(): T {
    return this
}

fun <T, R> T.applyValue(f: (T) -> R): R {
    return f(this)
}

fun omg() {
    val a: Output<List<A>>? = null

    a?.toJava()
}

class B

class A : ConvertibleToJava<B> {
    override fun toJava(): B {
        return B()
    }
}

suspend inline fun <T> T.applySuspend(block: T.() -> Unit): T {
    block()
    return this
}

interface ConvertibleToJava<T> {
    fun toJava(): T
}

object Pulumi {
    fun run(block: suspend (Context) -> Unit) {
        com.pulumi.Pulumi.run {
            runBlocking {
                block(it)
            }
        }
    }
}

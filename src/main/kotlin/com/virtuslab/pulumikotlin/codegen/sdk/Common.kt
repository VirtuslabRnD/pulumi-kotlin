package com.pulumi.kotlin

import com.pulumi.core.Output
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DslMarker
annotation class PulumiTagMarker


data class Resource(
    val whatever: String
)

data class ProviderResource(
    val whatever: String
)

data class CustomTimeouts(
    val whatever: String
)

data class ResourceTransformation(
    val whatever: String
)

data class Alias(
    val whatever: String
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
    val pluginDownloadURL: String? = null
)

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
    var pluginDownloadURL: String? = null
) {
    fun protect(value : Boolean): Unit { this.protect = value } 
    fun id(value : Output<String>?): Unit { this.id = value } 
    fun parent(value : Resource?): Unit { this.parent = value }
    fun dependsOn(value: Output<List<Resource>>?): Unit { this.dependsOn = value }
    fun ignoreChanges(value: List<String>?): Unit { this.ignoreChanges = value } 
    fun version(value : String?): Unit { this.version = value } 
    fun provider(value : ProviderResource?): Unit { this.provider = value }
    fun customTimeouts(value: CustomTimeouts?): Unit { this.customTimeouts = value }
    fun resourceTransformations(value: List<ResourceTransformation>?): Unit { this.resourceTransformations = value }
    fun aliases(value : List<Output<Alias>>?): Unit { this.aliases = value }
    fun urn(value : String?): Unit { this.urn = value } 
    fun deleteBeforeReplace(value: Boolean): Unit { this.deleteBeforeReplace = value }
    fun additionalSecretOutputs(value: List<String>?): Unit { this.additionalSecretOutputs = value } 
    fun importId(value: String?): Unit { this.importId = value } 
    fun replaceOnChanges(value: List<String>?): Unit { this.replaceOnChanges = value } 
    fun retainOnDelete(value: Boolean): Unit { this.retainOnDelete = value } 
    fun pluginDownloadURL(value: String?) = null
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
fun <T1, T2> Output<out Map<T1, ConvertibleToJava<T2>>>.toJava(): Output<Map<T1,T2>> {
    return applyValue { it.toJava() }
}

@JvmName("AB82F3249")
fun <T> Output<out List<ConvertibleToJava<T>>>.toJava(): Output<List<T>> {
    return applyValue { it.toJava() }
}

fun omg() {
    val a: Output<List<A>>? = null

    a?.toJava()
}

class B {

}

class A: ConvertibleToJava<B> {
    override fun toJava(): B {
        return B()
    }

}

suspend inline fun <T> T.applySuspend(block: suspend T.() -> Unit): T {
    block()
    return this
}

interface ConvertibleToJava<T> {
    fun toJava(): T
}

/* Copied from aws-java
*
* package com.pulumi.kotlin

import com.pulumi.core.Output
import com.pulumi.resources.CustomResourceOptions
import javax.annotation.Nullable

@DslMarker
annotation class PulumiTagMarker


data class Resource(
    val whatever: String
)

data class ProviderResource(
    val whatever: String
)

data class CustomTimeouts(
    val whatever: String
)

data class ResourceTransformation(
    val whatever: String
)

data class Alias(
    val whatever: String
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
    val pluginDownloadURL: String? = null
) {
    fun toJava(): CustomResourceOptions {
        return CustomResourceOptions.builder()
            .build()
    }
}

@PulumiTagMarker
data class CustomArgsBuilder(
    private var protect: Boolean = false,
    private var id: Output<String>? = null,
    private var parent: Resource? = null,
    private var dependsOn: Output<List<Resource>>? = null,
    private var ignoreChanges: List<String>? = null,
    private var version: String? = null,
    private var provider: ProviderResource? = null,
    private var customTimeouts: CustomTimeouts? = null,
    private var resourceTransformations: List<ResourceTransformation>? = null,
    private var aliases: List<Output<Alias>>? = null,
    private var urn: String? = null,
    private var deleteBeforeReplace: Boolean = false,
    private var additionalSecretOutputs: List<String>? = null,
    private var importId: String? = null,
    private var replaceOnChanges: List<String>? = null,
    private var retainOnDelete: Boolean = false,
    private var pluginDownloadURL: String? = null
) {
    fun protect(value : Boolean): Unit { this.protect = value }
    fun id(value : Output<String>?): Unit { this.id = value }
    fun parent(value : Resource?): Unit { this.parent = value }
    fun dependsOn(value: Output<List<Resource>>?): Unit { this.dependsOn = value }
    fun ignoreChanges(value: List<String>?): Unit { this.ignoreChanges = value }
    fun version(value : String?): Unit { this.version = value }
    fun provider(value : ProviderResource?): Unit { this.provider = value }
    fun customTimeouts(value: CustomTimeouts?): Unit { this.customTimeouts = value }
    fun resourceTransformations(value: List<ResourceTransformation>?): Unit { this.resourceTransformations = value }
    fun aliases(value : List<Output<Alias>>?): Unit { this.aliases = value }
    fun urn(value : String?): Unit { this.urn = value }
    fun deleteBeforeReplace(value: Boolean): Unit { this.deleteBeforeReplace = value }
    fun additionalSecretOutputs(value: List<String>?): Unit { this.additionalSecretOutputs = value }
    fun importId(value: String?): Unit { this.importId = value }
    fun replaceOnChanges(value: List<String>?): Unit { this.replaceOnChanges = value }
    fun retainOnDelete(value: Boolean): Unit { this.retainOnDelete = value }
    fun pluginDownloadURL(value: String?) = null

    fun build(): CustomArgs {
        return CustomArgs(
            protect = protect,
            id = id,
            parent = parent,
            dependsOn = dependsOn,
            ignoreChanges = ignoreChanges,
            version = version,
            provider = provider,
            customTimeouts = customTimeouts,
            resourceTransformations = resourceTransformations,
            aliases = aliases,
            urn = urn,
            deleteBeforeReplace = deleteBeforeReplace,
            additionalSecretOutputs = additionalSecretOutputs,
            importId = importId,
            replaceOnChanges = replaceOnChanges,
            retainOnDelete = retainOnDelete,
            pluginDownloadURL = pluginDownloadURL
        )
    }
}
* */
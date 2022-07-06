package xyz.mf7.kotlinpoet.sdk

import com.pulumi.core.Output
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
)
package com.pulumi.kotlin.options

import com.pulumi.core.Output
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.PulumiTagMarker
import com.pulumi.resources.CustomResourceOptions as JavaCustomResourceOptions

/**
 * A bag of optional settings that control a [com.pulumi.kotlin.KotlinCustomResource] behavior.
 */
class CustomResourceOptions internal constructor(
    private val javaBackingObject: JavaCustomResourceOptions,
) : ResourceOptions<JavaCustomResourceOptions>(javaBackingObject) {

    /**
     * The names of outputs for this resource that should be treated as secrets. This augments
     * the list that the resource provider and pulumi engine already determine based on inputs
     * to your resource. It can be used to mark certain outputs as a secret on a per-resource
     * basis.
     */
    val additionalSecretOutputs: List<String>?
        get() = javaBackingObject.additionalSecretOutputs

    /**
     * A resource may need to be replaced if an immutable property changes. In these cases,
     * cloud providers do not support updating an existing resource, so a new instance will be created
     * and the old one deleted. By default, to minimize downtime, Pulumi creates new instances of resources
     * before deleting old ones.
     *
     * Setting the [deleteBeforeReplace] option to true means that Pulumi will delete the existing resource
     * before creating its replacement. Be aware that this behavior has a cascading impact on dependencies,
     * so more resources may be replaced, which can lead to downtime. However, this option may be necessary
     * for some resources that manage scarce resources behind the scenes,
     * and/or resources that cannot exist side-by-side.
     */
    val deleteBeforeReplace: Boolean
        get() = javaBackingObject.deleteBeforeReplace

    /**
     * The [importId] resource option imports an existing cloud resource so that Pulumi can manage it.
     * Imported resources can have been provisioned by any other method, including manually in the cloud console
     * or with the cloud CLI.
     *
     * To import a resource, first specify the [importId] option with the resource's ID.
     * This ID is the same as would be returned by the id property for any resource created by Pulumi;
     * the ID is resource-specific. Pulumi reads the current state of the resource
     * with the given ID from the cloud provider. Next, you must specify all required arguments
     * to the resource constructor so that it exactly matches the state to import. By doing this,
     * you end up with a Pulumi program that will accurately generate a matching desired state.
     */
    val importId: String?
        get() = javaBackingObject.importId?.orElse(null)

    // generated resources use no args constructor as default value for options
    constructor() : this(JavaCustomResourceOptions.Empty)

    override fun toJava(): JavaCustomResourceOptions = javaBackingObject

    companion object {
        /**
         * Creates [CustomResourceOptions] with use of type-safe [CustomResourceOptionsBuilder].
         */
        suspend fun opts(block: suspend CustomResourceOptionsBuilder.() -> Unit): CustomResourceOptions {
            val customResourceOptionsBuilder = CustomResourceOptionsBuilder()
            block(customResourceOptionsBuilder)
            return customResourceOptionsBuilder.build()
        }
    }
}

/**
 * Builder for [CustomResourceOptions].
 */
@PulumiTagMarker
data class CustomResourceOptionsBuilder(
    var additionalSecretOutputs: List<String>? = null,
    override var aliases: List<Output<Alias>>? = null,
    override var customTimeouts: CustomTimeouts? = null,
    var deleteBeforeReplace: Boolean = false,
    override var dependsOn: Output<List<KotlinResource>>? = null,
    override var id: Output<String>? = null,
    override var ignoreChanges: List<String>? = null,
    var importId: String? = null,
    override var parent: KotlinResource? = null,
    override var pluginDownloadURL: String? = null,
    override var protect: Boolean = false,
    override var provider: KotlinProviderResource? = null,
    override var replaceOnChanges: List<String>? = null,
    override var resourceTransformations: List<ResourceTransformation>? = null,
    override var retainOnDelete: Boolean = false,
    override var urn: String? = null,
    override var version: String? = null,
    override var mergeWith: CustomResourceOptions? = null,
) : ResourceOptionsBuilder<CustomResourceOptions>(
    aliases,
    customTimeouts,
    dependsOn,
    id,
    ignoreChanges,
    parent,
    pluginDownloadURL,
    protect,
    provider,
    replaceOnChanges,
    resourceTransformations,
    retainOnDelete,
    urn,
    version,
    mergeWith,
) {

    /**
     * @see [CustomResourceOptions.additionalSecretOutputs]
     */
    fun additionalSecretOutputs(value: List<String>?) {
        this.additionalSecretOutputs = value
    }

    /**
     * @see [CustomResourceOptions.additionalSecretOutputs]
     */
    fun additionalSecretOutputs(vararg values: String) {
        this.additionalSecretOutputs = values.toList()
    }

    /**
     * @see [CustomResourceOptions.deleteBeforeReplace]
     */
    fun deleteBeforeReplace(value: Boolean) {
        this.deleteBeforeReplace = value
    }

    /**
     * @see [CustomResourceOptions.importId]
     */
    fun importId(value: String?) {
        this.importId = value
    }

    override fun build(): CustomResourceOptions {
        val javaBackingObject = JavaCustomResourceOptions.builder()
            .additionalSecretOutputs(additionalSecretOutputs)
            .aliases(aliases?.map { output -> output.applyValue { it.toJava() } })
            .customTimeouts(customTimeouts?.toJava())
            .deleteBeforeReplace(deleteBeforeReplace)
            .dependsOn(dependsOn?.applyValue { list -> list.map { it.underlyingJavaResource } })
            .id(id)
            .ignoreChanges(ignoreChanges)
            .importId(importId)
            .parent(parent?.underlyingJavaResource)
            .pluginDownloadURL(pluginDownloadURL)
            .protect(protect)
            .provider(provider?.underlyingJavaResource)
            .replaceOnChanges(replaceOnChanges)
            .resourceTransformations(resourceTransformations?.map { it.toJava() })
            .retainOnDelete(retainOnDelete)
            .urn(urn)
            .version(version)
            .build()

        return if (mergeWith == null) {
            CustomResourceOptions(javaBackingObject)
        } else {
            CustomResourceOptions(
                JavaCustomResourceOptions.merge(
                    mergeWith?.toJava(),
                    javaBackingObject,
                ),
            )
        }
    }
}

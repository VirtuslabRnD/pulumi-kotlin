package com.pulumi.kotlin.options

import com.pulumi.core.Output
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.PulumiTagMarker
import com.pulumi.resources.ProviderResource
import com.pulumi.resources.ComponentResourceOptions as JavaComponentResourceOptions

/**
 * A bag of optional settings that control a [com.pulumi.kotlin.KotlinComponentResource] behavior.
 */
class ComponentResourceOptions(
    private val javaBackingObject: JavaComponentResourceOptions,
) : ResourceOptions<JavaComponentResourceOptions>(javaBackingObject) {

    /**
     * An optional set of providers to use for child resources.
     * Note: do not provide both Provider and Providers.
     */
    val providers: List<ProviderResource>?
        get() = javaBackingObject.providers

    // generated resources use no args constructor as default value for options
    constructor() : this(JavaComponentResourceOptions.Empty)

    override fun toJava(): JavaComponentResourceOptions = javaBackingObject

    companion object {
        /**
         * Creates [ComponentResourceOptions] with use of type-safe [ComponentResourceOptionsBuilder].
         */
        suspend fun opts(block: suspend ComponentResourceOptionsBuilder.() -> Unit): ComponentResourceOptions {
            val componentResourceOptionsBuilder = ComponentResourceOptionsBuilder()
            block(componentResourceOptionsBuilder)
            return componentResourceOptionsBuilder.build()
        }
    }
}

/**
 * Builder for [ComponentResourceOptions].
 */
@PulumiTagMarker
data class ComponentResourceOptionsBuilder(
    override var aliases: List<Output<Alias>>? = null,
    override var customTimeouts: CustomTimeouts? = null,
    override var dependsOn: Output<List<KotlinResource>>? = null,
    override var id: Output<String>? = null,
    override var ignoreChanges: List<String>? = null,
    override var parent: KotlinResource? = null,
    override var pluginDownloadURL: String? = null,
    override var protect: Boolean = false,
    override var provider: KotlinProviderResource? = null,
    var providers: List<KotlinProviderResource>? = null,
    override var replaceOnChanges: List<String>? = null,
    override var resourceTransformations: List<ResourceTransformation>? = null,
    override var retainOnDelete: Boolean = false,
    override var urn: String? = null,
    override var version: String? = null,
    override var mergeWith: ComponentResourceOptions? = null,
) : ResourceOptionsBuilder<ComponentResourceOptions>(
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
     * @see [ComponentResourceOptions.providers]
     */
    fun providers(value: List<KotlinProviderResource>?) {
        this.providers = value
    }

    /**
     * @see [ComponentResourceOptions.providers]
     */
    fun providers(vararg value: KotlinProviderResource) {
        this.providers = value.toList()
    }

    override fun build(): ComponentResourceOptions {
        val javaBackingObject = JavaComponentResourceOptions.builder()
            .aliases(aliases?.map { output -> output.applyValue { it.toJava() } })
            .customTimeouts(customTimeouts?.toJava())
            .dependsOn(dependsOn?.applyValue { list -> list.map { it.underlyingJavaResource } })
            .id(id)
            .ignoreChanges(ignoreChanges)
            .parent(parent?.underlyingJavaResource)
            .pluginDownloadURL(pluginDownloadURL)
            .protect(protect)
            .provider(provider?.underlyingJavaResource)
            .providers(providers?.map { it.underlyingJavaResource })
            .replaceOnChanges(replaceOnChanges)
            .resourceTransformations(resourceTransformations?.map { it.toJava() })
            .retainOnDelete(retainOnDelete)
            .urn(urn)
            .version(version)
            .build()

        return if (mergeWith == null) {
            ComponentResourceOptions(javaBackingObject)
        } else {
            ComponentResourceOptions(
                JavaComponentResourceOptions.merge(
                    mergeWith?.toJava(),
                    javaBackingObject,
                ),
            )
        }
    }
}

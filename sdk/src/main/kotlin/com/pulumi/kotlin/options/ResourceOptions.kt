package com.pulumi.kotlin.options

import com.pulumi.core.Output
import com.pulumi.kotlin.ConvertibleToJava
import com.pulumi.kotlin.GlobalResourceMapper
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.resources.ResourceOptions as JavaResourceOptions

sealed class ResourceOptions<T : JavaResourceOptions>(private val javaBackingObject: T) : ConvertibleToJava<T> {

    /**
     * List of aliases for a resource or component resource. If you're changing the name, type,
     * or parent path of a resource or component resource, you can add the old name to the list of aliases
     * for a resource to ensure that existing resources will be migrated to the new name instead of being deleted
     * and replaced with the new named resource.
     *
     * The aliases option accepts a list of old identifiers. If a resource has been renamed multiple times,
     * it can have many aliases. The list of aliases may contain old [Alias] objects and/or old resource URNs.
     */
    val aliases: List<Output<Alias>>?
        get() = javaBackingObject.aliases?.map { it.applyValue { javaAlias -> Alias(javaAlias) } }

    /**
     * Set of custom timeouts for create, update, and delete operations on a resource.
     * These timeouts are specified using a duration string such as “5m” (5 minutes), “40s” (40 seconds),
     * or “1d” (1 day). Supported duration units are “ns”, “us” (or “µs”), “ms”, “s”, “m”, and “h”
     * (nanoseconds, microseconds, milliseconds, seconds, minutes, and hours, respectively).
     *
     * For the most part, Pulumi automatically waits for operations to complete and times out appropriately.
     * In some circumstances, such as working around bugs in the infrastructure provider,
     * custom timeouts may be necessary.
     *
     * The [customTimeouts] resource option does not apply to component resources,
     * and will not have the intended effect.
     */
    val customTimeouts: CustomTimeouts?
        get() = javaBackingObject.customTimeouts.map { CustomTimeouts(it) }.orElse(null)

    /**
     * The [dependsOn] resource option creates a list of explicit dependencies between resources.
     *
     *  Pulumi automatically tracks dependencies between resources when you supply an input argument
     *  that came from another resource's output properties. In some cases, however,
     *  you may need to explicitly specify additional dependencies that Pulumi doesn't know about
     *  but must still respect. This might happen if a dependency is external to the infrastructure itself,
     *  such as an application dependency—or is implied due to an ordering or eventual consistency requirement.
     *  The [dependsOn] option ensures that resource creation, update, and deletion operations
     *  are done in the correct order.
     */
    val dependsOn: Output<List<KotlinResource>>?
        get() = javaBackingObject.dependsOn?.applyValue { list -> list.mapNotNull { GlobalResourceMapper.tryMap(it) } }

    /**
     * An optional existing ID to load, rather than create.
     */
    val id: Output<String>?
        get() = javaBackingObject.id.orElse(null)

    /**
     * The [ignoreChanges] resource option specifies a list of properties
     * that Pulumi will ignore when it updates existing resources. Any properties specified in this list
     * that are also specified in the resource's arguments will only be used when creating the resource.
     */
    val ignoreChanges: List<String>?
        get() = javaBackingObject.ignoreChanges

    /**
     * Specifies a parent for a resource. It is used to associate children with the parents that encapsulate
     * or are responsible for them.
     */
    val parent: KotlinResource?
        get() = GlobalResourceMapper.tryMap(javaBackingObject.parent.orElse(null))

    /**
     * An optional URL, corresponding to the url from which the provider plugin that should be
     * used when operating on this resource is downloaded from. This URL overrides the download URL
     * inferred from the current package and should rarely be used.
     */
    val pluginDownloadURL: String?
        get() = javaBackingObject.pluginDownloadURL.orElse(null)

    /**
     * Marks a resource as protected. A protected resource cannot be deleted directly, and it will be an error
     * to do a Pulumi deployment which tries to delete a protected resource for any reason.
     *
     * To delete a protected resource, it must first be unprotected. There are two ways to unprotect a resource:
     * * Set `protect(false)` and then run `pulumi up`
     * * Use the `pulumi state unprotect` command
     *
     * Once the resource is unprotected, it can be deleted as part of a following update.
     *
     * The default is to inherit this value from the parent resource, and false for resources without a parent.
     */
    val protect: Boolean
        get() = javaBackingObject.isProtect

    /**
     * An optional provider to use for this resource's CRUD operations. If no provider is
     * supplied, the default provider for the resource's package will be used. The default
     * provider is pulled from the parent's provider bag.
     */
    val provider: KotlinProviderResource?
        get() = GlobalResourceMapper.tryMap(javaBackingObject.provider.orElse(null)) as KotlinProviderResource?

    /**
     * Indicates that changes to certain properties on a resource should force a replacement of the resource
     * instead of an in-place update.
     */
    val replaceOnChanges: List<String>?
        get() = javaBackingObject.replaceOnChanges

    /**
     * Provides a list of transformations to apply to a resource and all of its children.
     * This option is used to override or modify the inputs to the child resources of a component resource.
     * One example is to use the option to add other resource options (such as ignoreChanges or protect).
     * Another example is to modify an input property (such as adding to tags or changing a property
     * that is not directly configurable).
     *
     * Each transformation is a callback that gets invoked by the Pulumi runtime.
     * It receives the resource type, name, input properties, resource options, and the resource instance object itself.
     * The callback returns a new set of resource input properties and resource options that will be used
     * to construct the resource instead of the original values.
     */
    val resourceTransformations: List<ResourceTransformation>?
        get() = javaBackingObject.resourceTransformations?.map { jrt ->
            ResourceTransformation {
                jrt.apply(it).orElse(null)
            }
        }

    /**
     * Marks a resource to be retained. If this option is set then Pulumi will not call through
     * to the resource provider's `Delete` method when deleting or replacing the resource during `pulumi up`
     * or `pulumi destroy`. As a result, the resource will not be deleted from the backing cloud provider,
     * but will be removed from the Pulumi state.
     *
     * If a retained resource is deleted by Pulumi and you later want to actually delete it from the backing cloud
     * provider you will either need to use your provider's manual interface to find and delete the resource,
     * or import the resource back into Pulumi to unset [retainOnDelete] and delete it again fully.
     *
     * To actually delete a retained resource, this setting must first be set to `false`.
     */
    val retainOnDelete: Boolean
        get() = javaBackingObject.isRetainOnDelete

    /**
     * The URN of a previously-registered resource of this type to read from the engine.
     */
    val urn: String?
        get() = javaBackingObject.urn.orElse(null)

    /**
     * Specifies a provider version to use when operating on a resource. This version overrides the version
     * information inferred from the current package. This option should be used rarely.
     */
    val version: String?
        get() = javaBackingObject.version.orElse(null)
}

sealed class ResourceOptionsBuilder<T : ResourceOptions<*>>(
    open var aliases: List<Output<Alias>>? = null,
    open var customTimeouts: CustomTimeouts? = null,
    open var dependsOn: Output<List<KotlinResource>>? = null,
    open var id: Output<String>? = null,
    open var ignoreChanges: List<String>? = null,
    open var parent: KotlinResource? = null,
    open var pluginDownloadURL: String? = null,
    open var protect: Boolean = false,
    open var provider: KotlinProviderResource? = null,
    open var replaceOnChanges: List<String>? = null,
    open var resourceTransformations: List<ResourceTransformation>? = null,
    open var retainOnDelete: Boolean = false,
    open var urn: String? = null,
    open var version: String? = null,
    open var mergeWith: T? = null,
) {

    /**
     * @see [ResourceOptions.aliases]
     */
    fun aliases(vararg values: Alias) {
        aliases(values.map { Output.of(it) })
    }

    /**
     * @see [ResourceOptions.aliases]
     */
    fun aliases(vararg values: Output<Alias>) {
        aliases(values.toList())
    }

    /**
     * @see [ResourceOptions.aliases]
     */
    fun aliases(value: List<Output<Alias>>?) {
        this.aliases = value
    }

    /**
     * @see [ResourceOptions.aliases]
     */
    suspend fun aliases(vararg blocks: suspend AliasBuilder.() -> Unit) {
        this.aliases = blocks.map { alias(it) }.map { Output.of(it) }
    }

    /**
     * @see [ResourceOptions.customTimeouts]
     */
    fun customTimeouts(value: CustomTimeouts?) {
        this.customTimeouts = value
    }

    /**
     * @see [ResourceOptions.customTimeouts]
     */
    suspend fun customTimeouts(block: suspend CustomTimeoutsBuilder.() -> Unit) {
        val customTimeoutsBuilder = CustomTimeoutsBuilder()
        block(customTimeoutsBuilder)
        this.customTimeouts = customTimeoutsBuilder.build()
    }

    /**
     * @see [ResourceOptions.dependsOn]
     */
    fun dependsOn(vararg values: KotlinResource) {
        dependsOn(values.asList())
    }

    /**
     * @see [ResourceOptions.dependsOn]
     */
    fun dependsOn(values: List<KotlinResource>?) {
        this.dependsOn = Output.ofNullable(values)
    }

    /**
     * @see [ResourceOptions.dependsOn]
     */
    fun dependsOn(value: Output<List<KotlinResource>>?) {
        this.dependsOn = value
    }

    /**
     * @see [ResourceOptions.id]
     */
    fun id(value: String?) {
        id(Output.ofNullable(value))
    }

    /**
     * @see [ResourceOptions.id]
     */
    fun id(value: Output<String>?) {
        this.id = value
    }

    /**
     * @see [ResourceOptions.ignoreChanges]
     */
    fun ignoreChanges(vararg values: String) {
        ignoreChanges(values.toList())
    }

    /**
     * @see [ResourceOptions.ignoreChanges]
     */
    fun ignoreChanges(value: List<String>?) {
        this.ignoreChanges = value
    }

    /**
     * @see [ResourceOptions.parent]
     */
    fun parent(value: KotlinResource?) {
        this.parent = value
    }

    /**
     * @see [ResourceOptions.pluginDownloadURL]
     */
    fun pluginDownloadURL(value: String?) {
        this.pluginDownloadURL = value
    }

    /**
     * @see [ResourceOptions.protect]
     */
    fun protect(value: Boolean) {
        this.protect = value
    }

    /**
     * @see [ResourceOptions.provider]
     */
    fun provider(value: KotlinProviderResource?) {
        this.provider = value
    }

    /**
     * @see [ResourceOptions.replaceOnChanges]
     */
    fun replaceOnChanges(vararg values: String) {
        replaceOnChanges(values.toList())
    }

    /**
     * @see [ResourceOptions.replaceOnChanges]
     */
    fun replaceOnChanges(value: List<String>?) {
        this.replaceOnChanges = value
    }

    /**
     * @see [ResourceOptions.resourceTransformations]
     */
    fun resourceTransformations(vararg values: ResourceTransformation) {
        resourceTransformations(values.toList())
    }

    /**
     * @see [ResourceOptions.resourceTransformations]
     */
    fun resourceTransformations(values: List<ResourceTransformation>) {
        this.resourceTransformations = values
    }

    /**
     * @see [ResourceOptions.retainOnDelete]
     */
    fun retainOnDelete(value: Boolean) {
        this.retainOnDelete = value
    }

    /**
     * @see [ResourceOptions.urn]
     */
    fun urn(value: String?) {
        this.urn = value
    }

    /**
     * @see [ResourceOptions.version]
     */
    fun version(value: String?) {
        this.version = value
    }

    /**
     * Takes existing [T] values and prepares a type-safe builder
     * to merge the current [T] over the same properties of a given [T].
     *
     * The original option objects will be unchanged.
     *
     * Conceptually, property merging follows these basic rules:
     * * If the property is a collection, the final value will be a collection containing the
     * values from each option object.
     * * Simple scalar values from "options2" (i.e. "string", "int", "bool") will replace the values of "options1".
     * * "null" values in "options2" will be ignored.
     */
    fun mergeWith(opts: T?) {
        mergeWith = opts
    }

    internal abstract fun build(): T
}

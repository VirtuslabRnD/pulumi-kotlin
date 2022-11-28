// This class is included in the generated code. The package name matches its location in the generated code.
@file:Suppress("InvalidPackageDeclaration", "PackageDirectoryMismatch")

package com.pulumi.kotlin.options

import com.pulumi.core.Output
import com.pulumi.kotlin.ConvertibleToJava
import com.pulumi.kotlin.GlobalResourceMapper
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.PulumiTagMarker
import com.pulumi.kotlin.applyValue
import com.pulumi.kotlin.toJava
import com.pulumi.kotlin.toKotlin
import com.pulumi.resources.CustomResourceOptions as JavaCustomResourceOptions

/**
 * A bag of optional settings that control a [KotlinResource] behavior.
 */
class CustomResourceOptions internal constructor(
    private val javaBackingObject: JavaCustomResourceOptions,
) : ConvertibleToJava<JavaCustomResourceOptions> {

    /**
     * The names of outputs for this resource that should be treated as secrets. This augments
     * the list that the resource provider and pulumi engine already determine based on inputs
     * to your resource. It can be used to mark certain outputs as a secrets on a per-resource
     * basis.
     */
    val additionalSecretOutputs: List<String>?
        get() = javaBackingObject.additionalSecretOutputs

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
        get() = javaBackingObject.customTimeouts?.toKotlin()?.applyValue { CustomTimeouts(it) }

    /**
     * A resource may need to be replaced if an immutable property changes. In these cases,
     * cloud providers do not support updating an existing resource so a new instance will be created
     * and the old one deleted. By default, to minimize downtime, Pulumi creates new instances of resources
     * before deleting old ones.
     *
     * Setting the [deleteBeforeReplace] option to true means that Pulumi will delete the existing resource
     * before creating its replacement. Be aware that this behavior has a cascading impact on dependencies
     * so more resources may be replaced, which can lead to downtime. However, this option may be necessary
     * for some resources that manage scarce resources behind the scenes,
     * and/or resources that cannot exist side-by-side.
     */
    val deleteBeforeReplace: Boolean
        get() = javaBackingObject.deleteBeforeReplace

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
        get() = javaBackingObject.id?.toKotlin()

    /**
     * The [ignoreChanges] resource option specifies a list of properties
     * that Pulumi will ignore when it updates existing resources. Any properties specified in this list
     * that are also specified in the resource's arguments will only be used when creating the resource.
     */
    val ignoreChanges: List<String>?
        get() = javaBackingObject.ignoreChanges

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
        get() = javaBackingObject.importId?.toKotlin()

    /**
     * Specifies a parent for a resource. It is used to associate children with the parents that encapsulate
     * or are responsible for them.
     */
    val parent: KotlinResource?
        get() = javaBackingObject.parent?.toKotlin()?.applyValue { GlobalResourceMapper.tryMap(it) }

    /**
     * An optional URL, corresponding to the url from which the provider plugin that should be
     * used when operating on this resource is downloaded from. This URL overrides the download URL
     * inferred from the current package and should rarely be used.
     */
    val pluginDownloadURL: String?
        get() = javaBackingObject.pluginDownloadURL?.toKotlin()

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
        get() = javaBackingObject.provider?.toKotlin()
            ?.applyValue { GlobalResourceMapper.tryMap(it) } as KotlinProviderResource?

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
                jrt.apply(it).toKotlin()
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
        get() = javaBackingObject.urn?.toKotlin()

    /**
     * Specifies a provider version to use when operating on a resource. This version overrides the version
     * information inferred from the current package. This option should be used rarely.
     */
    val version: String?
        get() = javaBackingObject.version?.toKotlin()

    // generated resources use no args constructor as default value for options
    constructor() : this(JavaCustomResourceOptions.Empty)

    override fun toJava(): JavaCustomResourceOptions = javaBackingObject
}

/**
 * Builder for [CustomResourceOptions].
 */
@PulumiTagMarker
@Suppress("TooManyFunctions") // different overloads of method for the same property are required
data class CustomResourceOptionsBuilder(
    var additionalSecretOutputs: List<String>? = null,
    var aliases: List<Output<Alias>>? = null,
    var customTimeouts: CustomTimeouts? = null,
    var deleteBeforeReplace: Boolean = false,
    var dependsOn: Output<List<KotlinResource>>? = null,
    var id: Output<String>? = null,
    var ignoreChanges: List<String>? = null,
    var importId: String? = null,
    var parent: KotlinResource? = null,
    var pluginDownloadURL: String? = null,
    var protect: Boolean = false,
    var provider: KotlinProviderResource? = null,
    var replaceOnChanges: List<String>? = null,
    var resourceTransformations: List<ResourceTransformation>? = null,
    var retainOnDelete: Boolean = false,
    var urn: String? = null,
    var version: String? = null,
    var mergeWith: CustomResourceOptions? = null,
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
     * @see [CustomResourceOptions.aliases]
     */
    fun aliases(vararg values: Alias) {
        aliases(values.map { Output.of(it) })
    }

    /**
     * @see [CustomResourceOptions.aliases]
     */
    fun aliases(vararg values: Output<Alias>) {
        aliases(values.toList())
    }

    /**
     * @see [CustomResourceOptions.aliases]
     */
    fun aliases(value: List<Output<Alias>>?) {
        this.aliases = value
    }

    /**
     * @see [CustomResourceOptions.aliases]
     */
    suspend fun aliases(vararg blocks: suspend AliasBuilder.() -> Unit) {
        this.aliases = blocks.map { alias(it) }.map { Output.of(it) }
    }

    /**
     * @see [CustomResourceOptions.customTimeouts]
     */
    fun customTimeouts(value: CustomTimeouts?) {
        this.customTimeouts = value
    }

    /**
     * @see [CustomResourceOptions.customTimeouts]
     */
    suspend fun customTimeouts(block: suspend CustomTimeoutsBuilder.() -> Unit) {
        val customTimeoutsBuilder = CustomTimeoutsBuilder()
        block(customTimeoutsBuilder)
        this.customTimeouts = customTimeoutsBuilder.build()
    }

    /**
     * @see [CustomResourceOptions.deleteBeforeReplace]
     */
    fun deleteBeforeReplace(value: Boolean) {
        this.deleteBeforeReplace = value
    }

    /**
     * @see [CustomResourceOptions.dependsOn]
     */
    fun dependsOn(vararg values: KotlinResource) {
        dependsOn(values.asList())
    }

    /**
     * @see [CustomResourceOptions.dependsOn]
     */
    fun dependsOn(values: List<KotlinResource>?) {
        this.dependsOn = Output.ofNullable(values)
    }

    /**
     * @see [CustomResourceOptions.dependsOn]
     */
    fun dependsOn(value: Output<List<KotlinResource>>?) {
        this.dependsOn = value
    }

    /**
     * @see [CustomResourceOptions.id]
     */
    fun id(value: String?) {
        id(Output.ofNullable(value))
    }

    /**
     * @see [CustomResourceOptions.id]
     */
    fun id(value: Output<String>?) {
        this.id = value
    }

    /**
     * @see [CustomResourceOptions.ignoreChanges]
     */
    fun ignoreChanges(vararg values: String) {
        ignoreChanges(values.toList())
    }

    /**
     * @see [CustomResourceOptions.ignoreChanges]
     */
    fun ignoreChanges(value: List<String>?) {
        this.ignoreChanges = value
    }

    /**
     * @see [CustomResourceOptions.importId]
     */
    fun importId(value: String?) {
        this.importId = value
    }

    /**
     * @see [CustomResourceOptions.parent]
     */
    fun parent(value: KotlinResource?) {
        this.parent = value
    }

    /**
     * @see [CustomResourceOptions.pluginDownloadURL]
     */
    fun pluginDownloadURL(value: String?) {
        this.pluginDownloadURL = value
    }

    /**
     * @see [CustomResourceOptions.protect]
     */
    fun protect(value: Boolean) {
        this.protect = value
    }

    /**
     * @see [CustomResourceOptions.provider]
     */
    fun provider(value: KotlinProviderResource?) {
        this.provider = value
    }

    /**
     * @see [CustomResourceOptions.replaceOnChanges]
     */
    fun replaceOnChanges(vararg values: String) {
        replaceOnChanges(values.toList())
    }

    /**
     * @see [CustomResourceOptions.replaceOnChanges]
     */
    fun replaceOnChanges(value: List<String>?) {
        this.replaceOnChanges = value
    }

    /**
     * @see [CustomResourceOptions.resourceTransformations]
     */
    fun resourceTransformations(vararg values: ResourceTransformation) {
        resourceTransformations(values.toList())
    }

    /**
     * @see [CustomResourceOptions.resourceTransformations]
     */
    fun resourceTransformations(values: List<ResourceTransformation>) {
        this.resourceTransformations = values
    }

    /**
     * @see [CustomResourceOptions.retainOnDelete]
     */
    fun retainOnDelete(value: Boolean) {
        this.retainOnDelete = value
    }

    /**
     * @see [CustomResourceOptions.urn]
     */
    fun urn(value: String?) {
        this.urn = value
    }

    /**
     * @see [CustomResourceOptions.version]
     */
    fun version(value: String?) {
        this.version = value
    }

    /**
     * Takes existing [CustomResourceOptions] values and prepares type-safe builder
     * to merge current [CustomResourceOptions] over the same properties of given [CustomResourceOptions].
     *
     * The original options objects will be unchanged.
     *
     * Conceptually property merging follows these basic rules:
     * * If the property is a collection, the final value will be a collection containing the
     * values from each option object.
     * * Simple scalar values from "options2" (i.e. "string", "int", "bool") will replace the values of "options1".
     * * "null" values in "options2" will be ignored.
     */
    fun mergeWith(opts: CustomResourceOptions?) {
        mergeWith = opts
    }

    internal fun build(): CustomResourceOptions {
        val javaBackingObject = JavaCustomResourceOptions.builder()
            .additionalSecretOutputs(additionalSecretOutputs)
            .aliases(aliases?.map { it.toJava() })
            .customTimeouts(customTimeouts?.toJava())
            .deleteBeforeReplace(deleteBeforeReplace)
            .dependsOn(dependsOn?.toJava())
            .id(id)
            .ignoreChanges(ignoreChanges)
            .importId(importId)
            .parent(parent?.javaResource)
            .pluginDownloadURL(pluginDownloadURL)
            .protect(protect)
            .provider(provider?.javaResource)
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

/**
 * Creates [CustomResourceOptions] with use of type-safe [CustomResourceOptionsBuilder].
 */
suspend fun opts(block: suspend CustomResourceOptionsBuilder.() -> Unit): CustomResourceOptions {
    val customResourceOptionsBuilder = CustomResourceOptionsBuilder()
    block(customResourceOptionsBuilder)
    return customResourceOptionsBuilder.build()
}

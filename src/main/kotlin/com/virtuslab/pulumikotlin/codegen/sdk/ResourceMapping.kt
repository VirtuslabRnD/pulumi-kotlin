// This class is included in the generated code. The package name matches its location in the generated code.
@file:Suppress("InvalidPackageDeclaration", "PackageDirectoryMismatch", "unused")

package com.pulumi.kotlin

import com.pulumi.core.Output
import java.util.Optional

/**
 * Interface for creation of mappers for particular resource types.
 * It assumes that only subtypes of [com.pulumi.resources.Resource] from Java SDK can be mapped to Kotlin types.
 *
 * @param T Specific subtype of [KotlinResource] representing provider's resource in Kotlin SDK,
 *        mapper will produce objects of this type.
 */
interface ResourceMapper<out T : KotlinResource> {
    /**
     * Checks if given subtype of [com.pulumi.resources.Resource] can be mapped to type [T].
     *
     * @param javaResource Subtype of [com.pulumi.resources.Resource].
     * @return `true` if type of [javaResource] matches the type of [KotlinResource]'s backing object,
     *         `false` otherwise.
     */
    fun doesSupportMappingOfType(javaResource: com.pulumi.resources.Resource): Boolean

    /**
     * Creates instances of corresponding [KotlinResource] for given [com.pulumi.resources.Resource].
     *
     * @param javaResource Subtype of [com.pulumi.resources.Resource].
     * @return New instance of [KotlinResource]'s subtype, with given [javaResource] as backing object.
     */
    fun map(javaResource: com.pulumi.resources.Resource): T
}

/**
 * General mapper for mapping Resources backed by java objects ([com.pulumi.resources.Resource]).
 *
 * **In order to work properly, a Kotlin resource should be declared first with use of type-safe builder.
 * Only then a corresponding mapper will be registered in application's context.**
 */
internal object GeneralResourceMapper {
    private val mappers: MutableSet<ResourceMapper<KotlinResource>> = mutableSetOf()

    /**
     * Looks for corresponding [ResourceMapper] to given [javaResource] and maps it to proper [KotlinResource].
     *
     * @param javaResource Java backing object of a [KotlinResource].
     * @return Mapped [KotlinResource] or null, if given [javaResource] is null.
     */
    internal fun tryMap(javaResource: com.pulumi.resources.Resource?): KotlinResource? {
        if (javaResource == null) return null

        val mapper = mappers.find { it.doesSupportMappingOfType(javaResource) }
            ?: error("mapper for a type ${javaResource::class.java} was either not declared or not instantiated")

        return mapper.map(javaResource)
    }

    /**
     * If given [optionalJavaResource] is present, looks for corresponding [ResourceMapper]
     * and maps it to proper [KotlinResource].
     *
     * @param optionalJavaResource [Optional] with java backing object of a [KotlinResource].
     * @return Mapped [KotlinResource] or null if given [Optional] is empty (or contains null).
     */
    internal fun tryMap(optionalJavaResource: Optional<com.pulumi.resources.Resource?>?): KotlinResource? {
        return if (optionalJavaResource?.isPresent == true) tryMap(optionalJavaResource.get()) else null
    }

    /**
     * Looks for corresponding [ResourceMapper] to given [outputJavaResource]
     * and transforms it to proper [Output] with [KotlinResource].
     *
     * @param outputJavaResource [Output] with java backing object of a [KotlinResource].
     * @return Mapped [Output] of [KotlinResource] or of null, if given wrapped object is null.
     */
    internal fun tryMap(outputJavaResource: Output<com.pulumi.resources.Resource?>?): Output<KotlinResource?>? {
        return outputJavaResource?.applyValue { tryMap(it) }
    }

    /**
     * Adds given mapper to set of available mappers.
     *
     * @param mapper [ResourceMapper]
     * @return `true` if mapper was added, `false` if it already existed within the set.
     */
    internal fun registerMapper(mapper: ResourceMapper<KotlinResource>) = mappers.add(mapper)
}

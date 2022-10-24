// This class is included in the generated code. The package name matches its location in the generated code.
@file:Suppress("InvalidPackageDeclaration", "PackageDirectoryMismatch", "unused")

package com.pulumi.kotlin

import com.pulumi.core.Output
import java.util.Optional
import com.pulumi.resources.Resource as JavaResource

/**
 * Interface for creation of mappers for particular resource types.
 * It assumes that only subtypes of [JavaResource] from Java SDK can be mapped to Kotlin types.
 *
 * Type [T] is a specific subtype of [KotlinResource] representing provider's resource in Kotlin SDK,
 * mapper will produce objects of this type.
 */
interface ResourceMapper<out T : KotlinResource> {
    /**
     * Returns `true` if given subtype of [JavaResource] matches the type
     * of [KotlinResource]'s backing object (can be mapped to type [T]), `false` otherwise.
     */
    fun supportsMappingOfType(javaResource: JavaResource): Boolean

    /**
     * Creates new instance of corresponding [KotlinResource] for given [JavaResource],
     * with given [javaResource] as backing object.
     */
    fun map(javaResource: JavaResource): T
}

/**
 * General mapper for mapping Resources backed by java objects ([JavaResource]).
 *
 * **In order to work properly, a Kotlin resource should be declared first with use of type-safe builder.
 * Only then a corresponding mapper will be registered in application's context.**
 */
internal object GlobalResourceMapper {
    private val mappers: MutableList<ResourceMapper<KotlinResource>> = mutableListOf()

    /**
     * Looks for corresponding [ResourceMapper] to given [javaResource] and maps it to proper [KotlinResource].
     * Returns null, if given [javaResource] is null.
     */
    internal fun tryMap(javaResource: JavaResource?): KotlinResource? {
        if (javaResource == null) return null

        val mapper = requireNotNull(mappers.find { it.supportsMappingOfType(javaResource) }) {
            "mapper for a type ${javaResource::class.java} was either not declared or not instantiated"
        }

        return mapper.map(javaResource)
    }

    /**
     * If given [optionalJavaResource] is present, looks for corresponding [ResourceMapper]
     * and maps it to proper [KotlinResource]. Otherwise, returns null.
     */
    internal fun tryMap(optionalJavaResource: Optional<JavaResource?>?): KotlinResource? {
        return if (optionalJavaResource?.isPresent == true) tryMap(optionalJavaResource.get()) else null
    }

    /**
     * Looks for corresponding [ResourceMapper] to given [outputJavaResource]
     * and transforms it to proper [Output] with [KotlinResource].
     * Returned [Output] can be empty if given [outputJavaResource] is empty.
     */
    internal fun tryMap(outputJavaResource: Output<JavaResource?>?): Output<KotlinResource?>? {
        return outputJavaResource?.applyValue { tryMap(it) }
    }

    /**
     * Adds given mapper to set of available mappers.
     * Returns `true` if mapper was successfully added, `false` if it already existed within the internal collection.
     */
    internal fun registerMapper(mapper: ResourceMapper<KotlinResource>) = mappers.add(mapper)

    /**
     * Removes every registered mapper from internal collection.
     */
    internal fun clearMappers() = mappers.clear()
}

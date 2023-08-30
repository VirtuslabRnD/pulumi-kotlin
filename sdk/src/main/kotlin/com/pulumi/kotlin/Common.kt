package com.pulumi.kotlin

import com.pulumi.Context
import com.pulumi.core.Output
import kotlinx.coroutines.runBlocking
import com.pulumi.resources.ComponentResource as JavaComponentResource
import com.pulumi.resources.CustomResource as JavaCustomResource
import com.pulumi.resources.ProviderResource as JavaProviderResource
import com.pulumi.resources.Resource as JavaResource

@DslMarker
annotation class PulumiTagMarker

@Suppress("RedundantSuspendModifier")
suspend inline fun <T> T.applySuspend(block: T.() -> Unit): T {
    block()
    return this
}

interface ConvertibleToJava<T> {
    fun toJava(): T
}

/**
 * Parent class for resources within Kotlin SDK - equivalent to [JavaResource].
 *
 * Each resource within Kotlin SDK should have corresponding [ResourceMapper],
 * in order to properly translate Java resources to Kotlin representation.
 *
 * This class serves only as parent for all resources and should not be instantiated,
 * it cannot be sealed, because generated subclasses will be placed in other packages.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class KotlinResource private constructor(internal open val javaResource: JavaResource) {

    val pulumiResourceName: String
        get() = javaResource.pulumiResourceName()

    val pulumiResourceType: String
        get() = javaResource.pulumiResourceType()

    val urn: Output<String>
        get() = javaResource.urn()

    val pulumiChildResources: Set<KotlinResource>
        get() = javaResource.pulumiChildResources()
            .map {
                GlobalResourceMapper.tryMap(it)!!
            }
            .toSet()

    protected constructor(
        javaResource: JavaResource,
        mapper: ResourceMapper<KotlinResource>,
    ) : this(javaResource) {
        GlobalResourceMapper.registerMapper(mapper)
    }
}

/**
 * Parent class for component resources within Kotlin SDK - equivalent to [JavaComponentResource].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class KotlinComponentResource private constructor(
    override val javaResource: JavaComponentResource,
    mapper: ResourceMapper<KotlinResource>,
) : KotlinResource(javaResource, mapper)

/**
 * Parent class for custom resources within Kotlin SDK - equivalent to [JavaCustomResource].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class KotlinCustomResource internal constructor(
    override val javaResource: JavaCustomResource,
    mapper: ResourceMapper<KotlinResource>,
) : KotlinResource(javaResource, mapper) {
    val id: Output<String>
        get() = javaResource.id()
}

/**
 * Parent class for provider resources within Kotlin SDK - equivalent to [JavaProviderResource].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class KotlinProviderResource internal constructor(
    override val javaResource: JavaProviderResource,
    mapper: ResourceMapper<KotlinResource>,
) : KotlinResource(javaResource, mapper)

object Pulumi {

    /**
     * Run a Pulumi stack callback and wait for result.
     * In case of an error terminates the process with [System.exit].
     *
     * @param block the stack to run in Pulumi runtime
     */
    fun run(block: suspend (Context) -> Unit) {
        com.pulumi.Pulumi.run {
            runBlocking {
                block(it)
            }
        }
    }
}

/**
 * Append a value wrapped in an [Output] to exported stack outputs.
 * <p>
 * This method mutates the context internal state.
 * @param name name of the [Output]
 * @param value the value to be wrapped in [Output]
 * @return the current [Context]
 */
fun Context.export(name: String, value: Any): Context = export(name, Output.of(value))

package com.pulumi.kotlin.options

import com.pulumi.core.Output
import com.pulumi.kotlin.ConvertibleToJava
import com.pulumi.kotlin.GlobalResourceMapper
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.PulumiTagMarker
import com.pulumi.core.Alias as JavaAlias

/**
 * Alias is a description of prior name used for a resource. It can be processed in the
 * context of resource creation to determine what the full aliased URN would be.
 *
 * The presence of a property indicates if its value should be used.
 * If absent (i.e. "null"), then the value is not used.
 *
 * Note: because of the above, there needs to be special handling to indicate that the previous
 * "parent" of a [KotlinResource] was "null".
 * Specifically, pass in: [Alias.noParent]
 * @see [CustomResourceOptions.aliases]
 * @see [JavaAlias]
 */
class Alias internal constructor(private val javaBackingObject: JavaAlias) : ConvertibleToJava<JavaAlias> {

    /**
     * The previous urn to alias to. If this is provided, no other properties in this type should be provided.
     */
    val urn: String?
        get() = javaBackingObject.urn.orElse(null)

    /**
     * The previous name of the resource.
     * If empty, the current name of the resource is used.
     */
    val name: Output<String?>?
        get() = javaBackingObject.name.orElse(null)

    /**
     * The previous type of the resource. If empty, the current type of the resource is used.
     */
    val type: Output<String?>?
        get() = javaBackingObject.type.orElse(null)

    /**
     * The previous stack of the resource. If null, defaults to the value of `Pulumi.IDeployment.StackName`.
     */
    val stack: Output<String?>?
        get() = javaBackingObject.stack.orElse(null)

    /**
     * The previous project of the resource. If null, defaults to the value of `Pulumi.IDeployment.ProjectName`.
     */
    val project: Output<String?>?
        get() = javaBackingObject.project.orElse(null)

    /**
     * The previous parent of the resource. If null, the current parent of the resource is used.
     *
     * Only specify one of [Alias.parent] or [Alias.parentUrn] or [Alias.noParent].
     */
    val parent: KotlinResource?
        get() = GlobalResourceMapper.tryMap(javaBackingObject.parent)

    /**
     * The previous parent of the resource. If null, the current parent of the resource is used.
     *
     * Only specify one of [Alias.parent] or [Alias.parentUrn] or [Alias.noParent].
     */
    val parentUrn: Output<String?>?
        get() = javaBackingObject.parentUrn.orElse(null)

    /**
     * Used to indicate the resource previously had no parent. If `false` this property is ignored.
     *
     *  Only specify one of [Alias.parent] or [Alias.parentUrn] or [Alias.noParent].
     */
    val noParent: Boolean
        get() = javaBackingObject.hasNoParent()

    override fun toJava(): JavaAlias = javaBackingObject
}

/**
 * Builder for [Alias].
 */
@PulumiTagMarker
@Suppress("TooManyFunctions") // different overloads of method for the same property are required
class AliasBuilder internal constructor(
    var name: Output<String?>? = null,
    var type: Output<String?>? = null,
    var stack: Output<String?>? = null,
    var project: Output<String?>? = null,
    var parent: KotlinResource? = null,
    var parentUrn: Output<String?>? = null,
) {

    /**
     * @see [Alias.name]
     */
    fun name(value: Output<String?>?) {
        this.name = value
    }

    /**
     * @see [Alias.name]
     */
    fun name(value: String?) {
        this.name = Output.ofNullable(value)
    }

    /**
     * @see [Alias.type]
     */
    fun type(value: Output<String?>?) {
        this.type = value
    }

    /**
     * @see [Alias.type]
     */
    fun type(value: String?) {
        this.type = Output.ofNullable(value)
    }

    /**
     * @see [Alias.stack]
     */
    fun stack(value: Output<String?>?) {
        this.stack = value
    }

    /**
     * @see [Alias.stack]
     */
    fun stack(value: String?) {
        this.stack = Output.ofNullable(value)
    }

    /**
     * @see [Alias.project]
     */
    fun project(value: Output<String?>?) {
        this.project = value
    }

    /**
     * @see [Alias.project]
     */
    fun project(value: String?) {
        this.project = Output.ofNullable(value)
    }

    /**
     * @see [Alias.parent]
     */
    fun parent(value: KotlinResource?) {
        // TODO verify why java implementation mentions about requiring null in parentUrn but checks null in name
        //  requireNullState(name, () -> "Alias should not specify Alias#parent when Alias#parentUrn is  already.");
        //  @see https://github.com/VirtuslabRnD/jvm-lab/issues/54
        this.parent = value
    }

    /**
     * @see [Alias.parentUrn]
     */
    fun parentUrn(value: Output<String?>?) {
        // TODO verify why java implementation mentions about requiring null in parent but checks null in name
        //  requireNullState(name, () -> "Alias should not specify Alias#parent when Alias#parent is  already.");
        //  @see https://github.com/VirtuslabRnD/jvm-lab/issues/54
        this.parentUrn = value
    }

    internal fun build(): Alias {
        val javaAliasBuilder = JavaAlias.builder()

        // FIXME null-checks are necessary workaround for requireNullState(name) in java implementation,
        //  correct this code after fixing the issue in java
        if (name != null) javaAliasBuilder.name(name)
        if (type != null) javaAliasBuilder.type(type)
        if (stack != null) javaAliasBuilder.stack(stack)
        if (project != null) javaAliasBuilder.project(project)
        if (parent != null) javaAliasBuilder.parent(parent?.javaResource)
        if (parentUrn != null) javaAliasBuilder.parentUrn(parentUrn)

        val javaAlias = javaAliasBuilder.build()
        return Alias(javaAlias)
    }
}

/**
 * Creates [Alias] with use of type-safe [AliasBuilder].
 */
suspend fun alias(block: suspend AliasBuilder.() -> Unit): Alias {
    val aliasBuilder = AliasBuilder()
    block(aliasBuilder)
    return aliasBuilder.build()
}

/**
 * Creates [Alias] with empty properties and without parent (`noParent` set to `true`).
 *
 * @see [Alias.noParent]
 */
fun noParent(): Alias = Alias(JavaAlias.noParent())

/**
 * Creates [Alias] with given URN and other properties empty.
 *
 * @see [Alias.urn]
 */
fun withUrn(urn: String): Alias = Alias(JavaAlias.withUrn(urn))

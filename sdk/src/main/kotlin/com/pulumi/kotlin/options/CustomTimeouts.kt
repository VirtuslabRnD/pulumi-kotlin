package com.pulumi.kotlin.options

import com.pulumi.kotlin.ConvertibleToJava
import com.pulumi.kotlin.PulumiTagMarker
import java.util.Optional
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration
import com.pulumi.resources.CustomTimeouts as JavaCustomTimeouts

/**
 * Optional timeouts to supply in as [CustomResourceOptions.customTimeouts].
 *
 * @see [CustomResourceOptions.customTimeouts]
 * @see [JavaCustomTimeouts]
 */
class CustomTimeouts internal constructor(private val javaBackingObject: JavaCustomTimeouts) :
    ConvertibleToJava<JavaCustomTimeouts> {
    val create: Duration?
        get() = javaBackingObject.create.orElse(null)?.toKotlinDuration()
    val update: Duration?
        get() = javaBackingObject.update.orElse(null)?.toKotlinDuration()
    val delete: Duration?
        get() = javaBackingObject.delete.orElse(null)?.toKotlinDuration()

    override fun toJava(): JavaCustomTimeouts {
        return javaBackingObject
    }

    companion object {
        fun golangString(duration: Duration?): String {
            return JavaCustomTimeouts.golangString(Optional.ofNullable(duration?.toJavaDuration()))
        }
    }
}

/**
 * Builder for [CustomTimeouts]
 */
@PulumiTagMarker
class CustomTimeoutsBuilder(var create: Duration? = null, var update: Duration? = null, var delete: Duration? = null) {
    fun create(value: Duration?) {
        this.create = value
    }

    fun update(value: Duration?) {
        this.update = value
    }

    fun delete(value: Duration?) {
        this.delete = value
    }

    internal fun build(): CustomTimeouts {
        return CustomTimeouts(
            JavaCustomTimeouts(
                create?.toJavaDuration(),
                update?.toJavaDuration(),
                delete?.toJavaDuration(),
            ),
        )
    }
}

/**
 * Creates [CustomTimeouts] with use of type-safe [CustomTimeoutsBuilder].
 */
suspend fun customTimeouts(block: suspend CustomTimeoutsBuilder.() -> Unit): CustomTimeouts {
    val customTimeoutsBuilder = CustomTimeoutsBuilder()
    block(customTimeoutsBuilder)
    return customTimeoutsBuilder.build()
}

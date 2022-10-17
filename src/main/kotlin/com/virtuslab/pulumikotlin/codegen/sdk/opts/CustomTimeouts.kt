package com.pulumi.kotlin.options

import com.pulumi.kotlin.ConvertibleToJava
import com.pulumi.kotlin.PulumiTagMarker
import com.pulumi.kotlin.toKotlin
import java.util.Optional
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

/**
 * Optional timeouts to supply in as [CustomResourceOptions.customTimeouts].
 */
class CustomTimeouts internal constructor(private val javaBackingObject: com.pulumi.resources.CustomTimeouts) :
    ConvertibleToJava<com.pulumi.resources.CustomTimeouts> {
    val create: Duration?
        get() = javaBackingObject.create?.toKotlin()?.toKotlinDuration()
    val update: Duration?
        get() = javaBackingObject.update?.toKotlin()?.toKotlinDuration()
    val delete: Duration?
        get() = javaBackingObject.delete?.toKotlin()?.toKotlinDuration()

    override fun toJava(): com.pulumi.resources.CustomTimeouts {
        return javaBackingObject
    }

    companion object {
        fun golangString(duration: Duration?): String {
            return com.pulumi.resources.CustomTimeouts.golangString(Optional.ofNullable(duration?.toJavaDuration()))
        }
    }
}

@PulumiTagMarker
class CustomTimeoutsArgs(var create: Duration? = null, var update: Duration? = null, var delete: Duration? = null) {
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
            com.pulumi.resources.CustomTimeouts(
                create?.toJavaDuration(),
                update?.toJavaDuration(),
                delete?.toJavaDuration(),
            ),
        )
    }
}

suspend fun customTimeouts(block: suspend CustomTimeoutsArgs.() -> Unit): CustomTimeouts {
    val customTimeoutsArgs = CustomTimeoutsArgs()
    block(customTimeoutsArgs)
    return customTimeoutsArgs.build()
}

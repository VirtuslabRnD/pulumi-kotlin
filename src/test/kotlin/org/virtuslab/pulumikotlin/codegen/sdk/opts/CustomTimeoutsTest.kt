package org.virtuslab.pulumikotlin.codegen.sdk.opts

import com.pulumi.kotlin.options.customTimeouts
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

internal class CustomTimeoutsTest {

    @Test
    fun `create timout should be properly set`() = runBlocking {
        // given
        val duration = 100.milliseconds

        // when
        val customTimeouts = customTimeouts {
            create(duration)
        }

        // then
        assertEquals(duration, customTimeouts.create!!, "custom timeout create")
    }

    @Test
    fun `update timout should be properly set`() = runBlocking {
        // given
        val duration = 100.milliseconds

        // when
        val customTimeouts = customTimeouts {
            update(duration)
        }

        // then
        assertEquals(duration, customTimeouts.update!!, "custom timeout update")
    }

    @Test
    fun `delete timout should be properly set`() = runBlocking {
        // given
        val duration = 100.milliseconds

        // when
        val customTimeouts = customTimeouts {
            delete(duration)
        }

        // then
        assertEquals(duration, customTimeouts.delete!!, "custom timeout delete")
    }

    @Test
    fun `individual durations should be null in empty CustomTimeouts`() = runBlocking {
        // when
        val customTimeouts = customTimeouts {
        }

        assertAll(
            { assertNull(customTimeouts.create, "custom timeout create") },
            { assertNull(customTimeouts.update, "custom timeout update") },
            { assertNull(customTimeouts.delete, "custom timeout delete") },
        )
    }
}

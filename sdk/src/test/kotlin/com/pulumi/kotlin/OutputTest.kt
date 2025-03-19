package com.pulumi.kotlin

import com.pulumi.core.Output
import com.pulumi.core.internal.OutputData
import com.pulumi.core.internal.OutputInternal
import com.pulumi.kotlin.Output.interpolation
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutputTest {

    @Test
    fun `interpolates known outputs`() {
        // given
        val output1 = Output.of("value1")
        val output2 = Output.of("value2")
        val output3 = Output.of("value3")

        // when
        val result = runBlocking {
            interpolation {
                "output1: ${+output1}, output2: ${+output2}, output3: ${+output3}"
            }
        }

        // then
        assertEquals("output1: value1, output2: value2, output3: value3", result.getValue())
        assertFalse(result.isSecret())
        assertTrue(result.isKnown())

        val javaResult = Output.format("output1: %s, output2: %s, output3: %s", output1, output2, output3)
        assertEquals(javaResult.getValue(), result.getValue())
        assertEquals(javaResult.isKnown(), result.isKnown())
        assertEquals(javaResult.isSecret(), result.isSecret())
    }

    @Test
    fun `interpolates unknown outputs`() {
        // given
        val output1 = Output.of("value1")
        val output2 = unknownOutput()
        val output3 = Output.of("value3")

        // when
        val result = runBlocking {
            interpolation {
                "output1: ${+output1}, output2: ${+output2}, output3: ${+output3}"
            }
        }

        // then
        assertEquals(null, result.getValue())
        assertFalse(result.isKnown())
        assertFalse(result.isSecret())

        val javaResult = Output.format("output1: %s, output2: %s, output3: %s", output1, output2, output3)
        assertEquals(javaResult.getValue(), result.getValue())
        assertEquals(javaResult.isKnown(), result.isKnown())
        assertEquals(javaResult.isSecret(), result.isSecret())
    }

    @Test
    fun `interpolates secret outputs`() {
        // given
        val output1 = Output.of("value1")
        val output2 = Output.ofSecret("value2")
        val output3 = Output.of("value3")

        // when
        val result = runBlocking {
            interpolation {
                "output1: ${+output1}, output2: ${+output2}, output3: ${+output3}"
            }
        }

        // then
        assertEquals("output1: value1, output2: value2, output3: value3", result.getValue())
        assertTrue(result.isSecret())
        assertTrue(result.isKnown())

        val javaResult = Output.format("output1: %s, output2: %s, output3: %s", output1, output2, output3)
        assertEquals(javaResult.getValue(), result.getValue())
        assertEquals(javaResult.isKnown(), result.isKnown())
        assertEquals(javaResult.isSecret(), result.isSecret())
    }

    @Test
    fun `interpolates unknown and secret outputs`() {
        // given
        val output1 = unknownOutput()
        val output2 = Output.ofSecret("value2")
        val output3 = unknownOutput()

        // when
        val result = runBlocking {
            interpolation {
                "output1: ${+output1}, output2: ${+output2}, output3: ${+output3}"
            }
        }

        // then
        assertEquals(null, result.getValue())
        assertFalse(result.isKnown())
        assertTrue(result.isSecret())

        val javaResult = Output.format("output1: %s, output2: %s, output3: %s", output1, output2, output3)
        assertEquals(javaResult.getValue(), result.getValue())
        assertEquals(javaResult.isKnown(), result.isKnown())
        assertEquals(javaResult.isSecret(), result.isSecret())
    }

    @Test
    fun `interpolates outputs that are both unknown and secret`() {
        // given
        val output1 = Output.of("value1")
        val output2 = unknownOutput().asSecret()
        val output3 = Output.of("value3")

        // when
        val result = runBlocking {
            interpolation {
                "output1: ${+output1}, output2: ${+output2}, output3: ${+output3}"
            }
        }

        // then
        assertEquals(null, result.getValue())
        assertFalse(result.isKnown())
        assertTrue(result.isSecret())

        val javaResult = Output.format("output1: %s, output2: %s, output3: %s", output1, output2, output3)
        assertEquals(javaResult.getValue(), result.getValue())
        assertEquals(javaResult.isKnown(), result.isKnown())
        assertEquals(javaResult.isSecret(), result.isSecret())
    }

    private fun unknownOutput(): Output<String> {
        return OutputInternal(CompletableFuture.completedFuture(OutputData.unknown()))
    }

    private fun Output<String>.getValue(): String? {
        return (this as OutputInternal<String>)
            .dataAsync
            .get()
            .valueNullable
    }

    private fun Output<String>.isKnown(): Boolean {
        return (this as OutputInternal<String>)
            .isKnown
            .get()
    }

    private fun Output<String>.isSecret(): Boolean {
        return (this as OutputInternal<String>)
            .isSecret
            .get()
    }
}

package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapWithKeyTransformer.ConflictsNotAllowed
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertSame

internal class MapWithKeyTransformerTest {
    @Test
    fun `lowercaseKeys works for string key`() {
        val baseMap = mapOf("A" to "B", "b" to "c")

        val transformedMap = baseMap.lowercaseKeys()

        assertAll(
            { assertEquals("B", transformedMap["a"]) },
            { assertEquals("B", transformedMap["A"]) },
            { assertEquals("c", transformedMap["b"]) },
            { assertEquals("c", transformedMap["B"]) },
        )
    }

    @Test
    fun `lowercaseKeys works for Pair key`() {
        val baseMap = mapOf(Pair("a", "B") to "B")

        val transformedMap = baseMap.transformKeys { it.copy(first = it.first.lowercase()) }

        assertAll(
            { assertEquals("B", transformedMap[Pair("A", "B")]) },
            { assertEquals("B", transformedMap[Pair("a", "B")]) },
            { assertNull(transformedMap[Pair("a", "b")]) },
            { assertNull(transformedMap[Pair("A", "b")]) },
        )
    }

    @Test
    fun `lowercaseKeys should not allow mapping to identical keys if values differ (conflicts)`() {
        val baseMap = mapOf("a" to "B", "A" to "c")

        assertThrows<ConflictsNotAllowed> {
            baseMap.lowercaseKeys()
        }
    }

    @Test
    fun `lowercaseKeys should allow mapping to identical keys if values are the same (references)`() {
        class A
        val value1 = A()

        val baseMap = mapOf("a" to value1, "A" to value1)

        val transformedMap = baseMap.lowercaseKeys()

        assertAll(
            { assertSame(value1, transformedMap["a"]) },
            { assertSame(value1, transformedMap["A"]) },
        )
    }

    @Test
    fun `lowercaseKeys should allow mapping to identical keys if values are the same (structural equality)`() {
        val baseMap = mapOf("a" to "B", "A" to "B")

        val transformedMap = baseMap.lowercaseKeys()

        assertAll(
            { assertEquals("B", transformedMap["a"]) },
            { assertEquals("B", transformedMap["A"]) },
        )
    }
}

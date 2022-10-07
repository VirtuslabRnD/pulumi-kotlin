package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapWithKeyTransformer.ConflictsNotAllowed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MapWithKeyTransformerTest {
    @Test
    fun `lowercaseKeys works for string key`() {
        val baseMap = mapOf("A" to "B", "b" to "c")

        val transformedMap = baseMap.lowercaseKeys()

        assertEquals("B", transformedMap["a"])
        assertEquals("B", transformedMap["A"])
        assertEquals("c", transformedMap["b"])
        assertEquals("c", transformedMap["B"])
    }

    @Test
    fun `lowercaseKeys works for Pair key`() {
        val baseMap = mapOf(Pair("a", "B") to "B")

        val transformedMap = baseMap.transformKeys { it.copy(first = it.first.lowercase()) }

        assertEquals("B", transformedMap[Pair("A", "B")])
        assertEquals("B", transformedMap[Pair("a", "B")])
        assertNull(transformedMap[Pair("a", "b")])
        assertNull(transformedMap[Pair("A", "b")])
    }

    @Test
    fun `lowercaseKeys should not allow mapping to identical keys (conflicts)`() {
        val baseMap = mapOf("a" to "B", "A" to "c")

        assertThrows<ConflictsNotAllowed> {
            baseMap.lowercaseKeys()
        }
    }
}

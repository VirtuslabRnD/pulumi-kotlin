package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step2intermediate.SetWithKeyTransformer.ConflictsNotAllowed
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SetWithKeyTransformerTest {
    @Test
    fun `transformKeys works for string key`() {
        val baseSet = setOf("A") + anyStringSet()

        val transformedSet = baseSet.transformKeys { it.lowercase() }

        assertTrue(transformedSet.contains("A"))
        assertTrue(transformedSet.contains("a"))
    }

    @Test
    fun `lowercaseKeys works for Pair key`() {
        val baseSet = setOf(Pair("a", "B")) + anyPairSet()

        val transformedSet = baseSet.transformKeys { it.copy(first = it.first.lowercase()) }

        assertTrue(transformedSet.contains(Pair("a", "B")))
        assertTrue(transformedSet.contains(Pair("A", "B")))
        assertFalse(transformedSet.contains(Pair("A", "b")))
        assertFalse(transformedSet.contains(Pair("a", "b")))
    }

    @Test
    fun `lowercaseKeys should not allow mapping to identical keys (conflicts)`() {
        val baseSet = setOf("a", "A") + anyStringSet()

        assertThrows<ConflictsNotAllowed> {
            baseSet.transformKeys { it.lowercase() }
        }
    }

    private fun anyPairSet() =
        setOf(Pair("ignore1", "ignore2"), Pair("ignore3", "ignore4"))

    private fun anyStringSet() =
        setOf("ignore1", "ignore2")
}

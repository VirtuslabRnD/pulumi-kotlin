package org.virtuslab.pulumikotlin.codegen.step2intermediate

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.virtuslab.pulumikotlin.assertDoesNotContain
import kotlin.test.assertContains

internal class SetWithKeyTransformerTest {
    @Test
    fun `transformKeys works for string key`() {
        val baseSet = setOf("A") + anyStringSet()

        val transformedSet = baseSet.transformKeys { it.lowercase() }

        assertAll(
            { assertContains(transformedSet, "a") },
            { assertContains(transformedSet, "A") },
        )
    }

    @Test
    fun `transformKeys works for Pair key`() {
        val baseSet = setOf(Pair("a", "B")) + anyPairSet()

        val transformedSet = baseSet.transformKeys { it.copy(first = it.first.lowercase()) }

        assertAll(
            { assertContains(transformedSet, Pair("a", "B")) },
            { assertContains(transformedSet, Pair("A", "B")) },
            { assertDoesNotContain(transformedSet, Pair("A", "b")) },
            { assertDoesNotContain(transformedSet, Pair("a", "b")) },
        )
    }

    @Test
    fun `transformKeys should allow mapping to identical keys`() {
        val baseSet = setOf("a", "A", "a") + anyStringSet()

        val transformedSet = baseSet.transformKeys { it.lowercase() }

        assertAll(
            { assertContains(transformedSet, "a") },
            { assertContains(transformedSet, "A") },
        )
    }

    private fun anyPairSet() =
        setOf(Pair("ignore1", "ignore2"), Pair("ignore3", "ignore4"))

    private fun anyStringSet() =
        setOf("ignore1", "ignore2")
}

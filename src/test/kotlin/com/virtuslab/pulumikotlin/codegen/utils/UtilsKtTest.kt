package com.virtuslab.pulumikotlin.codegen.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtilsKtTest {
    @Test
    fun `capitalize should change the first letter`() {
        assertEquals("Word", "word".capitalize())
    }

    @Test
    fun `capitalize should be ok if the string is already capitalized`() {
        assertEquals("Word", "Word".capitalize())
    }

    @Test
    fun `decapitalize should change the first letter`() {
        assertEquals("word", "Word".decapitalize())
    }

    @Test
    fun `decapitalize should be ok if the string is already decapitalized`() {
        assertEquals("word", "Word".decapitalize())
    }

    @Test
    fun `letIf should invoke the mapper if its first argument is set to true`() {
        val result = StringBuilder()
            .append("letIf")
            .letIf(true) {
                it.append(" works!")
            }
            .toString()

        assertEquals("letIf works!", result)
    }

    @Test
    fun `letIf should not invoke the mapper if its first argument is set to false`() {
        val result = StringBuilder()
            .append("letIf works!")
            .letIf(false) {
                it.append(" (not really)")
            }
            .toString()

        assertEquals("letIf works!", result)
    }

    @Test
    fun `filterNotNullValues removes null values from any Map`() {
        val mapWithNullValues = mapOf("a" to null, "b" to "c")

        val mapAfterFiltering = mapWithNullValues.filterNotNullValues()

        assertEquals(mapOf("b" to "c"), mapAfterFiltering)
    }

    @Test
    fun `valuesToSet takes any Grouping and creates Map (where values are of type Set) `() {
        val list = listOf(
            "a" to 1,
            "a" to 2,
            "b" to 1,
            "c" to 1,
            "c" to 1,
        )

        val mapAfterGrouping = list
            .groupingBy { it.second }
            .valuesToSet { it.first }

        val expectedMap = mapOf(
            1 to setOf("a", "b", "c"),
            2 to setOf("a"),
        )
        assertEquals(expectedMap, mapAfterGrouping)
    }
}

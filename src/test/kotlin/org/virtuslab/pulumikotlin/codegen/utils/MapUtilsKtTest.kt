package org.virtuslab.pulumikotlin.codegen.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MapUtilsKtTest {
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

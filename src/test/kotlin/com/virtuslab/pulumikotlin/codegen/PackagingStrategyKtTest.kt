package com.virtuslab.pulumikotlin.codegen

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class PackagingStrategyKtTest {


    companion object {
        @JvmStatic
        private fun groupingTestParameters(): List<Arguments> = listOf(
            Arguments.of(

                mapOf(
                    "a" to listOf("b", "d"),
                    "c" to listOf("d"),
                    "g" to listOf("b")
                ),

                mapOf(
                    "b" to setOf("a", "g"),
                    "d" to setOf("c", "a")
                )

            ),
        )
    }

    @ParameterizedTest
    @MethodSource("groupingTestParameters")
    fun groupingTest(input: Map<String, List<String>>, expected: Map<String, Set<String>>) {
        val computedGrouping = input.grouping { list -> list }

        assertEquals(
            expected,
            computedGrouping
        )
    }
}
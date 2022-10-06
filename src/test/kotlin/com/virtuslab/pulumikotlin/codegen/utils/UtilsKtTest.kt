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
        val stringBuilder = StringBuilder("letIf")

        val result = stringBuilder
            .letIf(true) {
                it.append(" works!")
            }
            .toString()

        assertEquals("letIf works!", result)
    }

    @Test
    fun `letIf should not invoke the mapper if its first argument is set to false`() {
        val stringBuilder = StringBuilder("letIf works!")

        val result = stringBuilder
            .letIf(false) {
                it.append(" (not really)")
            }
            .toString()

        assertEquals("letIf works!", result)
    }
}

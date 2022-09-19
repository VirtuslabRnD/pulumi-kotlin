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
}

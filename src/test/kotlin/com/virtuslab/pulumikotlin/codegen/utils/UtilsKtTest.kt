package com.virtuslab.pulumikotlin.codegen.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtilsKtTest {
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
}

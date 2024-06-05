package org.virtuslab.pulumikotlin.codegen.step3codegen.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals

internal class EnumValueGeneratorTest {

    @Suppress("unused", "ktlint:standard:enum-entry-name-case", "EnumNaming", "EnumEntryName")
    enum class EnumValueTestCase(
        val input: String,
        val expected: String,
    ) {
        `should unwrap asterisk`("*", "Asterisk"),
        `should unwrap number`("1", "One"),
        `should replace colon with underscore and escape invalid initial character`("8.3", "_8_3"),
        `should escape invalid initial character`("11", "_11"),
        `should remove dashes`("Microsoft-Windows-Shell-Startup", "MicrosoftWindowsShellStartup"),
        `should replace colon with underscore`("Microsoft.Batch", "Microsoft_Batch"),
        `should escape Java keyword and capitalize enum value`("final", "Final_"),
        `should replace commas and spaces with underscores`(
            "SystemAssigned, UserAssigned",
            "SystemAssigned_UserAssigned",
        ),
        `should replace parentheses with underscores`("Dev(NoSLA)_Standard_D11_v2", "Dev_NoSLA_Standard_D11_v2"),
        `should replace plus sign with underscores`("Standard_E8as_v4+1TB_PS", "Standard_E8as_v4_1TB_PS"),
    }

    @ParameterizedTest
    @EnumSource(EnumValueTestCase::class)
    fun `should create a valid Java enum value`(testCase: EnumValueTestCase) {
        assertEquals(
            testCase.expected,
            EnumValueGenerator.makeSafeEnumName(testCase.input),
        )
    }

    @Test
    fun `should throw exception when trying to turn an invalid token into enum value`() {
        val exception = assertThrows<IllegalStateException> {
            EnumValueGenerator.makeSafeEnumName("+")
        }

        assertEquals(
            "Cannot construct an enum with name `+`",
            exception.message,
        )
    }
}

package com.virtuslab.pulumikotlin.codegen.step2intermediate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PulumiNameTest {

    @Test
    fun `toFunctionGroupObjectName should return a proper object name when targeting Java`() {
        val name = PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority")
        val result = name.toFunctionGroupObjectName(
            NamingFlags(
                InputOrOutput.Input,
                UseCharacteristic.FunctionRoot,
                LanguageType.Java,
            ),
        )

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `toFunctionGroupObjectName should return a proper object name when targeting Kotlin`() {
        val name = PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority")
        val result = name.toFunctionGroupObjectName(
            NamingFlags(
                InputOrOutput.Input,
                UseCharacteristic.FunctionRoot,
                LanguageType.Kotlin,
            ),
        )

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `toFunctionGroupObjectPackage should return a proper object name when targeting Java`() {
        val name = PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority")
        val result = name.toFunctionGroupObjectPackage(
            NamingFlags(
                InputOrOutput.Input,
                UseCharacteristic.FunctionRoot,
                LanguageType.Java,
            ),
        )

        assertEquals("com.pulumi.aws.acmpca", result)
    }

    @Test
    fun `toFunctionGroupObjectPackage should return a proper object name when targeting Kotlin`() {
        val name = PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority")
        val result = name.toFunctionGroupObjectPackage(
            NamingFlags(
                InputOrOutput.Input,
                UseCharacteristic.FunctionRoot,
                LanguageType.Kotlin,
            ),
        )

        assertEquals("com.pulumi.aws.acmpca.kotlin", result)
    }
}

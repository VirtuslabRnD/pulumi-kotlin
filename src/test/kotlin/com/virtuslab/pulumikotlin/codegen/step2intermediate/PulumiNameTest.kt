package com.virtuslab.pulumikotlin.codegen.step2intermediate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PulumiNameTest {

    enum class ValidNameCase(val inputName: String, val expectedPulumiName: PulumiName) {
        Case1TypeName(
            "aws:acm/CertificateOptions:CertificateOptions",
            PulumiName("aws", listOf("acm"), "CertificateOptions"),
        ),
        Case2ResourceName(
            "aws:acm/certificate:Certificate",
            PulumiName("aws", listOf("acm"), "Certificate"),
        ),
        Case3FunctionName(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            PulumiName("aws", listOf("acmpca"), "getCertificateAuthority"),
        ),
        Case4LongFunctionName(
            "aws:acmpca/getCertificateAuthorityRevocationConfigurationCrlConfiguration:getCertificateAuthorityRevocationConfigurationCrlConfiguration",
            PulumiName("aws", listOf("acmpca"), "getCertificateAuthorityRevocationConfigurationCrlConfiguration"),
        ),
    }

    @ParameterizedTest
    @EnumSource(ValidNameCase::class)
    fun `PulumiName#from correctly parses the name`(case: ValidNameCase) {
        val pulumiName = PulumiName.from(case.inputName)
        assertEquals(case.expectedPulumiName, pulumiName)
    }

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

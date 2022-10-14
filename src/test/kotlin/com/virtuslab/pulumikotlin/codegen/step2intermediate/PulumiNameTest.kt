package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PulumiNameTest {

    @Suppress("unused")
    enum class ValidNameCase(val inputName: String, val expectedPulumiName: PulumiName) {
        TypeName(
            "aws:acm/CertificateOptions:CertificateOptions",
            PulumiName("aws", listOf("acm"), "CertificateOptions"),
        ),
        ResourceName(
            "aws:acm/certificate:Certificate",
            PulumiName("aws", listOf("acm"), "Certificate"),
        ),
        FunctionName(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            PulumiName("aws", listOf("acmpca"), "getCertificateAuthority"),
        ),
        LongFunctionName(
            @Suppress("MaxLineLength")
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

        val result = name.toFunctionGroupObjectName(NamingFlags(Root, Function, Input, Java))

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `toFunctionGroupObjectName should return a proper object name when targeting Kotlin`() {
        val name = PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority")

        val result = name.toFunctionGroupObjectName(NamingFlags(Root, Function, Input, Kotlin))

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `toFunctionGroupObjectPackage should return a proper object name when targeting Java`() {
        val name = PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority")

        val result = name.toFunctionGroupObjectPackage(NamingFlags(Root, Function, Input, Java))

        assertEquals("com.pulumi.aws.acmpca", result)
    }

    @Test
    fun `toFunctionGroupObjectPackage should return a proper object name when targeting Kotlin`() {
        val name = PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority")

        val result = name.toFunctionGroupObjectPackage(NamingFlags(Root, Function, Input, Kotlin))

        assertEquals("com.pulumi.aws.acmpca.kotlin", result)
    }

    @Test
    fun `package and class names are correctly generated for function from index namespace`() {
        val pulumiName = PulumiName.from("github:index/getActionsPublicKey:getActionsPublicKey")

        val kotlinNamingFlags = NamingFlags(Root, Function, Input, Kotlin)
        val javaNamingFlags = NamingFlags(Root, Function, Input, Java)

        assertEquals(
            "com.pulumi.github.kotlin",
            pulumiName.toFunctionGroupObjectPackage(kotlinNamingFlags),
        )
        assertEquals(
            "GithubFunctions",
            pulumiName.toFunctionGroupObjectName(kotlinNamingFlags),
        )

        assertEquals(
            "com.pulumi.github",
            pulumiName.toFunctionGroupObjectPackage(javaNamingFlags),
        )
        assertEquals(
            "GithubFunctions",
            pulumiName.toFunctionGroupObjectName(javaNamingFlags),
        )
    }

    @Test
    fun `package names are correctly generated for resource from index namespace`() {
        val pulumiName = PulumiName.from("github:index/actionsEnvironmentSecret:ActionsEnvironmentSecret")

        val kotlinNamingFlags = NamingFlags(Root, Resource, Input, Kotlin)
        val javaNamingFlags = NamingFlags(Root, Resource, Input, Java)

        assertEquals(
            "com.pulumi.github.kotlin",
            pulumiName.toResourcePackage(kotlinNamingFlags),
        )

        assertEquals(
            "com.pulumi.github",
            pulumiName.toResourcePackage(javaNamingFlags),
        )
    }

    @Test
    fun `package names are correctly generated for types from index namespace`() {
        val pulumiName = PulumiName.from("github:index/ProviderAppAuth:ProviderAppAuth")

        assertEquals(
            "com.pulumi.github.kotlin.inputs",
            pulumiName.toPackage(NamingFlags(Nested, Resource, Input, Kotlin)),
        )
        assertEquals(
            "com.pulumi.github.inputs",
            pulumiName.toPackage(NamingFlags(Nested, Resource, Input, Java)),
        )

        assertEquals(
            "com.pulumi.github.kotlin",
            pulumiName.toPackage(NamingFlags(Root, Resource, Input, Kotlin)),
        )
        assertEquals(
            "com.pulumi.github",
            pulumiName.toPackage(NamingFlags(Root, Resource, Input, Java)),
        )

        assertEquals(
            "com.pulumi.github.kotlin.inputs",
            pulumiName.toPackage(NamingFlags(Root, Function, Input, Kotlin)),
        )
        assertEquals(
            "com.pulumi.github.inputs",
            pulumiName.toPackage(NamingFlags(Root, Function, Input, Java)),
        )

        assertEquals(
            "com.pulumi.github.kotlin.outputs",
            pulumiName.toPackage(NamingFlags(Root, Resource, Output, Kotlin)),
        )
        assertEquals(
            "com.pulumi.github.outputs",
            pulumiName.toPackage(NamingFlags(Root, Resource, Output, Java)),
        )

        assertEquals(
            "com.pulumi.github.kotlin.outputs",
            pulumiName.toPackage(NamingFlags(Root, Function, Output, Kotlin)),
        )
        assertEquals(
            "com.pulumi.github.outputs",
            pulumiName.toPackage(NamingFlags(Root, Function, Output, Java)),
        )
    }
}

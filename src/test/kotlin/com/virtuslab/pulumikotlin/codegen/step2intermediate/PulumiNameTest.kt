package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.namingConfigurationWithSlashInModuleFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PulumiNameTest {

    @Suppress("unused")
    enum class ValidNameCase(
        val inputName: String,
        val namingConfiguration: PulumiNamingConfiguration,
        val expectedPulumiName: PulumiName,
    ) {
        TypeName(
            "aws:acm/CertificateOptions:CertificateOptions",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
        ),
        ResourceName(
            "aws:acm/certificate:Certificate",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "Certificate"),
        ),
        FunctionName(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName("aws", listOf("com", "pulumi", "aws", "acmpca"), "getCertificateAuthority"),
        ),
        LongFunctionName(
            @Suppress("MaxLineLength") "aws:acmpca/getCertificateAuthorityRevocationConfigurationCrlConfiguration:getCertificateAuthorityRevocationConfigurationCrlConfiguration",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName(
                "aws",
                listOf("com", "pulumi", "aws", "acmpca"),
                "getCertificateAuthorityRevocationConfigurationCrlConfiguration",
            ),
        ),
    }

    @Suppress("unused")
    enum class ValidEnumModifiers(
        val receiverPulumiName: PulumiName,
        val inputNamingFlags: NamingFlags,
        val expectedPackageWithModifiers: String,
    ) {
        KotlinResourceInput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Resource, Input, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),
        KotlinResourceOutput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Resource, Output, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),
        KotlinFunctionInput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Function, Input, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),
        KotlinFunctionOutput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Function, Output, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),

        JavaResourceInput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Resource, Input, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
        JavaResourceOutput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Resource, Output, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
        JavaFunctionInput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Function, Input, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
        JavaFunctionOutput(
            PulumiName("aws", listOf("com", "pulumi", "aws", "acm"), "CertificateOptions"),
            NamingFlags(Nested, Function, Output, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
    }

    @ParameterizedTest
    @EnumSource(ValidNameCase::class)
    fun `PulumiName#from correctly parses the name`(case: ValidNameCase) {
        val pulumiName = PulumiName.from(case.inputName, case.namingConfiguration)

        assertEquals(case.expectedPulumiName, pulumiName)
    }

    @ParameterizedTest
    @EnumSource(ValidEnumModifiers::class)
    fun `package modifiers for enums are correctly resolved`(case: ValidEnumModifiers) {
        // when
        val actualPackageWithModifiers = case.receiverPulumiName.toPackage(case.inputNamingFlags)

        // then
        assertEquals(case.expectedPackageWithModifiers, actualPackageWithModifiers)
    }

    @Test
    fun `toFunctionGroupObjectName should return a proper object name when targeting Java`() {
        val name = PulumiName.from(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val result = name.toFunctionGroupObjectName(NamingFlags(Root, Function, Input, Java))

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `toFunctionGroupObjectName should return a proper object name when targeting Kotlin`() {
        val name = PulumiName.from(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val result = name.toFunctionGroupObjectName(NamingFlags(Root, Function, Input, Kotlin))

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `toFunctionGroupObjectPackage should return a proper object name when targeting Java`() {
        val name = PulumiName.from(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val result = name.toFunctionGroupObjectPackage(NamingFlags(Root, Function, Input, Java))

        assertEquals("com.pulumi.aws.acmpca", result)
    }

    @Test
    fun `toFunctionGroupObjectPackage should return a proper object name when targeting Kotlin`() {
        val name = PulumiName.from(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val result = name.toFunctionGroupObjectPackage(NamingFlags(Root, Function, Input, Kotlin))

        assertEquals("com.pulumi.aws.acmpca.kotlin", result)
    }

    @Test
    fun `package and class names are correctly generated for function from index namespace`() {
        val pulumiName = PulumiName.from(
            "github:index/getActionsPublicKey:getActionsPublicKey",
            namingConfigurationWithSlashInModuleFormat("github"),
        )

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
        val pulumiName = PulumiName.from(
            "github:index/actionsEnvironmentSecret:ActionsEnvironmentSecret",
            namingConfigurationWithSlashInModuleFormat("github"),
        )

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
        val pulumiName = PulumiName.from(
            "github:index/ProviderAppAuth:ProviderAppAuth",
            namingConfigurationWithSlashInModuleFormat("github"),
        )

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

    @Test
    fun `PulumiName is correctly created as per given naming configuration`() {
        // given
        val token = "provider:module***objectName:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration(
            providerName = "provider",
            moduleFormat = "(.*)(?:\\*{3}[^\\*]*)",
            basePackage = "org.example",
        )

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.packageProviderName) },
            { assertEquals(listOf("org", "example", "provider", "module"), pulumiName.namespace) },
            { assertEquals("ObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `PulumiName correctly resolves name overrides`() {
        // given
        val token = "provider:module:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration(
            providerName = "provider",
            basePackage = "org.example",
            packageOverrides = mapOf("module" to "overrideModule", "ObjectName" to "OverrideObjectName"),
        )

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.packageProviderName) },
            { assertEquals(listOf("org", "example", "provider", "overrideModule"), pulumiName.namespace) },
            { assertEquals("OverrideObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `PulumiName is correctly created with default naming configuration`() {
        // given
        val token = "provider:module:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.packageProviderName) },
            { assertEquals(listOf("com", "pulumi", "provider", "module"), pulumiName.namespace) },
            { assertEquals("ObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `PulumiName ignores module 'providers'`() {
        // given
        val token = "provider:providers:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.packageProviderName) },
            { assertEquals(listOf("com", "pulumi", "provider"), pulumiName.namespace) },
            { assertEquals("ObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `malformed token causes an exception`() {
        // given
        val token = "provider/module:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration(providerName = "provider")

        // when - then
        assertThrows<IllegalStateException>("Malformed token should throw an exception") {
            PulumiName.from(token, namingConfiguration)
        }
    }
}

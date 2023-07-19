package org.virtuslab.pulumikotlin.codegen.step2intermediate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import org.virtuslab.pulumikotlin.codegen.utils.DEFAULT_PROVIDER_TOKEN
import org.virtuslab.pulumikotlin.namingConfigurationWithSlashInModuleFormat

internal class PulumiNameTest {

    @Suppress("unused", "MaxLineLength")
    enum class ValidNameCase(
        val inputName: String,
        val namingConfiguration: PulumiNamingConfiguration,
        val expectedPulumiName: PulumiName,
    ) {
        TypeName(
            "aws:acm/CertificateOptions:CertificateOptions",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
        ),
        ResourceName(
            "aws:acm/certificate:Certificate",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "Certificate", false),
        ),
        FunctionName(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName("aws", null, listOf("com", "pulumi"), "acmpca", "getCertificateAuthority", false),
        ),
        LongFunctionName(
            "aws:acmpca/getCertificateAuthorityRevocationConfigurationCrlConfiguration:getCertificateAuthorityRevocationConfigurationCrlConfiguration",
            namingConfigurationWithSlashInModuleFormat("aws"),
            PulumiName(
                "aws",
                null,
                listOf("com", "pulumi"),
                "acmpca",
                "getCertificateAuthorityRevocationConfigurationCrlConfiguration",
                false,
            ),
        ),
    }

    @ParameterizedTest
    @EnumSource(ValidNameCase::class)
    fun `PulumiName#from correctly parses the name`(case: ValidNameCase) {
        val pulumiName = PulumiName.from(case.inputName, case.namingConfiguration)

        assertEquals(case.expectedPulumiName, pulumiName)
    }

    @Suppress("unused")
    enum class ValidEnumModifiers(
        val receiverPulumiName: PulumiName,
        val inputNamingFlags: NamingFlags,
        val expectedPackageWithModifiers: String,
    ) {
        KotlinResourceInput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Resource, Input, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),
        KotlinResourceOutput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Resource, Output, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),
        KotlinFunctionInput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Function, Input, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),
        KotlinFunctionOutput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Function, Output, Kotlin, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.kotlin.enums",
        ),

        JavaResourceInput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Resource, Input, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
        JavaResourceOutput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Resource, Output, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
        JavaFunctionInput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Function, Input, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
        JavaFunctionOutput(
            PulumiName("aws", null, listOf("com", "pulumi"), "acm", "CertificateOptions", false),
            NamingFlags(Nested, Function, Output, Java, GeneratedClass.EnumClass),
            "com.pulumi.aws.acm.enums",
        ),
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
    fun `function class name is correct when targeting Java`() {
        val name = PulumiName.from(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val result = name.toFunctionGroupObjectName(NamingFlags(Root, Function, Input, Java))

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `function class name is correct when targeting Kotlin`() {
        val name = PulumiName.from(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val result = name.toFunctionGroupObjectName(NamingFlags(Root, Function, Input, Kotlin))

        assertEquals("AcmpcaFunctions", result)
    }

    @Test
    fun `function class and package in index namespace are correct for Kotlin`() {
        val name = PulumiName.from(
            "aws:index/getElasticIpFilter:getElasticIpFilter",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val namingFlags = NamingFlags(Root, Function, Input, Kotlin)
        val className = name.toFunctionGroupObjectName(namingFlags)
        val packageName = name.toPackage(namingFlags)

        assertEquals("AwsFunctions", className)
        assertEquals("com.pulumi.aws.kotlin.inputs", packageName)
    }

    @Test
    fun `function class and package in index namespace are correct for Java`() {
        val name = PulumiName.from(
            "aws:index/getElasticIpFilter:getElasticIpFilter",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val namingFlags = NamingFlags(Root, Function, Input, Java)
        val className = name.toFunctionGroupObjectName(namingFlags)
        val packageName = name.toPackage(namingFlags)

        assertEquals("AwsFunctions", className)
        assertEquals("com.pulumi.aws.inputs", packageName)
    }

    @Test
    fun `function class and package in index namespace with dash in provider name are correct for Kotlin`() {
        val name = PulumiName.from(
            "equinix-metal:index/getVolumeSnapshotPolicy:getVolumeSnapshotPolicy",
            namingConfigurationWithSlashInModuleFormat("equinix-metal"),
        )

        val namingFlags = NamingFlags(Root, Function, Input, Kotlin)
        val className = name.toFunctionGroupObjectName(namingFlags)
        val packageName = name.toPackage(namingFlags)

        assertEquals("EquinixMetalFunctions", className)
        assertEquals("com.pulumi.equinixmetal.kotlin.inputs", packageName)
    }

    @Test
    fun `function class and package in index namespace with dash in provider name are correct for Java`() {
        val name = PulumiName.from(
            "equinix-metal:index/getVolumeSnapshotPolicy:getVolumeSnapshotPolicy",
            namingConfigurationWithSlashInModuleFormat("equinix-metal"),
        )

        val namingFlags = NamingFlags(Root, Function, Input, Java)
        val className = name.toFunctionGroupObjectName(namingFlags)
        val packageName = name.toPackage(namingFlags)

        assertEquals("EquinixmetalFunctions", className)
        assertEquals("com.pulumi.equinixmetal.inputs", packageName)
    }

    @Test
    fun `function class and package in index namespace with overridden provider name are correct for Kotlin`() {
        val name = PulumiName.from(
            "equinix-metal:index/getVolumeSnapshotPolicy:getVolumeSnapshotPolicy",
            namingConfigurationWithSlashInModuleFormat("equinix-metal", mapOf("equinix-metal" to "equinixmetal")),
        )

        val namingFlags = NamingFlags(Root, Function, Input, Kotlin)
        val className = name.toFunctionGroupObjectName(namingFlags)
        val packageName = name.toPackage(namingFlags)

        assertEquals("EquinixMetalFunctions", className)
        assertEquals("com.pulumi.equinixmetal.kotlin.inputs", packageName)
    }

    @Test
    fun `function class and package in index namespace with overridden provider name are correct for Java`() {
        val name = PulumiName.from(
            "equinix-metal:index/getVolumeSnapshotPolicy:getVolumeSnapshotPolicy",
            namingConfigurationWithSlashInModuleFormat("equinix-metal", mapOf("equinix-metal" to "equinixmetal")),
        )

        val namingFlags = NamingFlags(Root, Function, Input, Java)
        val className = name.toFunctionGroupObjectName(namingFlags)
        val packageName = name.toPackage(namingFlags)

        assertEquals("EquinixmetalFunctions", className)
        assertEquals("com.pulumi.equinixmetal.inputs", packageName)
    }

    @Test
    fun `function package is correct when targeting Java`() {
        val name = PulumiName.from(
            "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
            namingConfigurationWithSlashInModuleFormat("aws"),
        )

        val result = name.toFunctionGroupObjectPackage(NamingFlags(Root, Function, Input, Java))

        assertEquals("com.pulumi.aws.acmpca", result)
    }

    @Test
    fun `function package is correct when targeting Kotlin`() {
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
        val token = "provider:moduleXDobjectName:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration.create(
            providerName = "provider",
            moduleFormat = "(.*)(?:XD[^\\*]*)",
            basePackage = "org.example",
        )

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.providerName) },
            { assertEquals(listOf("org", "example", "provider", "module"), pulumiName.namespace) },
            { assertEquals("ObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `PulumiName correctly resolves name overrides`() {
        // given
        val token = "provider:module:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration.create(
            providerName = "provider",
            basePackage = "org.example",
            packageOverrides = mapOf("module" to "overrideModule", "provider" to "overrideProvider"),
        )

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.providerName) },
            {
                assertEquals(
                    listOf("org", "example", "overrideProvider", "overrideModule"),
                    pulumiName.namespace,
                )
            },
            { assertEquals("ObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `PulumiName is correctly created with default naming configuration`() {
        // given
        val token = "provider:module:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration.create(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.providerName) },
            { assertEquals(listOf("com", "pulumi", "provider", "module"), pulumiName.namespace) },
            { assertEquals("ObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `PulumiName ignores module 'providers'`() {
        // given
        val token = "provider:providers:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration.create(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)

        // then
        assertAll(
            { assertEquals("provider", pulumiName.providerName) },
            { assertEquals(listOf("com", "pulumi", "provider"), pulumiName.namespace) },
            { assertEquals("ObjectName", pulumiName.name) },
        )
    }

    @Test
    fun `malformed token causes an exception`() {
        // given
        val token = "provider/module:ObjectName"
        val namingConfiguration = PulumiNamingConfiguration.create(providerName = "provider")

        // when - then
        val exception = assertThrows<IllegalArgumentException> {
            PulumiName.from(token, namingConfiguration)
        }

        assertEquals(
            "Malformed token $token",
            exception.message,
        )
    }

    @Test
    fun `provider class and package are correct when targeting Kotlin`() {
        val name = PulumiName.from(
            DEFAULT_PROVIDER_TOKEN,
            namingConfigurationWithSlashInModuleFormat("aws"),
            isProvider = true,
        )

        val namingFlags = NamingFlags(Root, Resource, Input, Kotlin)
        val className = name.toResourceName(namingFlags)
        val packageName = name.toResourcePackage(namingFlags)

        assertEquals("AwsProvider", className)
        assertEquals("com.pulumi.aws.kotlin", packageName)
    }

    @Test
    fun `provider class and package are correct when targeting Java`() {
        val name = PulumiName.from(
            DEFAULT_PROVIDER_TOKEN,
            namingConfigurationWithSlashInModuleFormat("aws"),
            isProvider = true,
        )

        val namingFlags = NamingFlags(Root, Resource, Input, Java)
        val className = name.toResourceName(namingFlags)
        val packageName = name.toResourcePackage(namingFlags)

        assertEquals("Provider", className)
        assertEquals("com.pulumi.aws", packageName)
    }

    @Test
    fun `provider class and package with dash in provider name are correct when targeting Kotlin`() {
        val name = PulumiName.from(
            DEFAULT_PROVIDER_TOKEN,
            namingConfigurationWithSlashInModuleFormat("equinix-metal"),
            isProvider = true,
        )

        val namingFlags = NamingFlags(Root, Resource, Input, Kotlin)
        val className = name.toResourceName(namingFlags)
        val packageName = name.toResourcePackage(namingFlags)

        assertEquals("EquinixMetalProvider", className)
        assertEquals("com.pulumi.equinixmetal.kotlin", packageName)
    }

    @Test
    fun `provider class and package with dash in provider name are correct when targeting Java`() {
        val name = PulumiName.from(
            DEFAULT_PROVIDER_TOKEN,
            namingConfigurationWithSlashInModuleFormat("equinix-metal"),
            isProvider = true,
        )

        val namingFlags = NamingFlags(Root, Resource, Input, Java)
        val className = name.toResourceName(namingFlags)
        val packageName = name.toResourcePackage(namingFlags)

        assertEquals("Provider", className)
        assertEquals("com.pulumi.equinixmetal", packageName)
    }

    @Test
    fun `provider class and package with overridden provider name are correct when targeting Kotlin`() {
        val name = PulumiName.from(
            DEFAULT_PROVIDER_TOKEN,
            namingConfigurationWithSlashInModuleFormat("equinix-metal", mapOf("equinix-metal" to "equinixmetal")),
            isProvider = true,
        )

        val namingFlags = NamingFlags(Root, Resource, Input, Kotlin)
        val className = name.toResourceName(namingFlags)
        val packageName = name.toResourcePackage(namingFlags)

        assertEquals("EquinixMetalProvider", className)
        assertEquals("com.pulumi.equinixmetal.kotlin", packageName)
    }

    @Test
    fun `provider class and package with overridden provider name are correct when targeting Java`() {
        val name = PulumiName.from(
            DEFAULT_PROVIDER_TOKEN,
            namingConfigurationWithSlashInModuleFormat("equinix-metal", mapOf("equinix-metal" to "equinixmetal")),
            isProvider = true,
        )

        val namingFlags = NamingFlags(Root, Resource, Input, Java)
        val className = name.toResourceName(namingFlags)
        val packageName = name.toResourcePackage(namingFlags)

        assertEquals("Provider", className)
        assertEquals("com.pulumi.equinixmetal", packageName)
    }

    @Test
    fun `a resource function name is decapitalized correctly`() {
        // given
        val token = "provider:module:OrganizationPolicy"
        val namingConfiguration = PulumiNamingConfiguration.create(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)
        val namingFlags = NamingFlags(Root, Resource, Input, Java)
        val resourceFunctionName = pulumiName.toResourceFunctionName(namingFlags)

        // then
        assertEquals("organizationPolicy", resourceFunctionName)
    }

    @Test
    fun `a resource function name starting with a three-letter acronym is decapitalized correctly`() {
        // given
        val token = "provider:module:IAMBinding"
        val namingConfiguration = PulumiNamingConfiguration.create(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)
        val namingFlags = NamingFlags(Root, Resource, Input, Java)
        val resourceFunctionName = pulumiName.toResourceFunctionName(namingFlags)

        // then
        assertEquals("iamBinding", resourceFunctionName)
    }

    @Test
    fun `a resource function name that is a three-letter acronym is decapitalized correctly`() {
        // given
        val token = "provider:module:CRL"
        val namingConfiguration = PulumiNamingConfiguration.create(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)
        val namingFlags = NamingFlags(Root, Resource, Input, Java)
        val resourceFunctionName = pulumiName.toResourceFunctionName(namingFlags)

        // then
        assertEquals("crl", resourceFunctionName)
    }

    @Test
    fun `a resource function name starting with a two-letter acronym and number is decapitalized correctly`() {
        // given
        val token = "provider:module:EC2Fleet"
        val namingConfiguration = PulumiNamingConfiguration.create(providerName = "provider")

        // when
        val pulumiName = PulumiName.from(token, namingConfiguration)
        val namingFlags = NamingFlags(Root, Resource, Input, Java)
        val resourceFunctionName = pulumiName.toResourceFunctionName(namingFlags)

        // then
        assertEquals("ec2Fleet", resourceFunctionName)
    }
}

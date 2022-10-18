package com.virtuslab.pulumikotlin.codegen.sdk.opts

import com.pulumi.core.Output
import com.pulumi.kotlin.GeneralResourceMapper
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.options.ResourceTransformation
import com.pulumi.kotlin.options.alias
import com.pulumi.kotlin.options.customTimeouts
import com.pulumi.kotlin.options.noParent
import com.pulumi.kotlin.options.opts
import com.pulumi.kotlin.options.transformationResult
import com.pulumi.kotlin.options.withUrn
import com.virtuslab.pulumikotlin.assertOutputPropertyEqual
import com.virtuslab.pulumikotlin.concat
import com.virtuslab.pulumikotlin.extractOutputValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.reflect.KClass
import kotlin.test.assertContentEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class CustomResourceOptionsTest {

    @AfterEach
    fun cleanUpAfterTests() {
        unmockkAll()
    }

    @Test
    fun `option additionalSecretOutputs should be properly set`() = runBlocking {
        // given
        val additionalSecretOutputs = listOf("username", "password")

        // when
        val optsCreatedWithList = opts {
            additionalSecretOutputs(additionalSecretOutputs)
        }
        val optsCreatedWithVararg = opts {
            additionalSecretOutputs(*additionalSecretOutputs.toTypedArray())
        }

        // then
        assertAll(
            {
                assertContentEquals(
                    optsCreatedWithList.additionalSecretOutputs,
                    additionalSecretOutputs,
                    "additionalSecretOutputs created with list arg",
                )
            },
            {
                assertContentEquals(
                    optsCreatedWithVararg.additionalSecretOutputs,
                    additionalSecretOutputs,
                    "additionalSecretOutputs created with list arg",
                )
            },
        )
    }

    @Test
    fun `option aliases should be properly set`() = runBlocking {
        // given
        val aliasUrn = "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket"
        val aliasName = "old-resource-name"
        val alias = alias {
            name(aliasName)
        }
        val outputAlias = Output.of(alias)
        val outputAliasesList = listOf(outputAlias)

        // when
        val optsCreatedWithMultipleMethods = opts {
            aliases(
                alias {
                    name(aliasName)
                },
                withUrn(aliasUrn),
                noParent(),
                alias,
            )
        }
        val optsCreatedWithVarargs = opts {
            aliases(
                alias,
                alias,
            )
        }
        val optsCreatedWithOutputVarargs = opts {
            aliases(outputAlias)
        }
        val optsCreatedWithList = opts {
            aliases(outputAliasesList)
        }

        // then
        val assertionsForAliases = concat(
            optsCreatedWithVarargs.aliases,
            optsCreatedWithOutputVarargs.aliases,
            optsCreatedWithList.aliases,
        )
            .map { extractOutputValue(it) }
            .map { extractedAlias ->
                assertOutputPropertyEqual(
                    extractedAlias,
                    aliasName,
                    "alias name",
                ) { it?.name }
            }
            .toTypedArray()

        // then
        assertAll(
            { assertEquals(optsCreatedWithMultipleMethods.aliases!!.size, 4) },
            { assertTrue { assertionsForAliases.isNotEmpty() } },
            *assertionsForAliases,
        )
    }

    @Test
    fun `option customTimeouts should be properly set`() = runBlocking {
        // given
        val duration = 100.toDuration(DurationUnit.MILLISECONDS)
        val customTimeouts = customTimeouts { create(duration); update(duration); delete(duration) }

        // when
        val optsCreatedWithArgs = opts {
            customTimeouts { create(duration); update(duration); delete(duration) }
        }
        val optsCreatedWithObject = opts {
            customTimeouts(customTimeouts)
        }

        // then
        assertAll(
            {
                assertEquals(
                    optsCreatedWithArgs.customTimeouts!!.create,
                    duration,
                    "opts.customTimeouts.create created with args",
                )
            },
            {
                assertEquals(
                    optsCreatedWithArgs.customTimeouts!!.update,
                    duration,
                    "opts.customTimeouts.update created with args",
                )
            },
            {
                assertEquals(
                    optsCreatedWithArgs.customTimeouts!!.delete,
                    duration,
                    "opts.customTimeouts.delete created with args",
                )
            },

            {
                assertEquals(
                    optsCreatedWithObject.customTimeouts!!.create,
                    duration,
                    "opts.customTimeouts.create created with object",
                )
            },
            {
                assertEquals(
                    optsCreatedWithObject.customTimeouts!!.update,
                    duration,
                    "opts.customTimeouts.update created with object",
                )
            },
            {
                assertEquals(
                    optsCreatedWithObject.customTimeouts!!.delete,
                    duration,
                    "opts.customTimeouts.delete created with object",
                )
            },
        )
    }

    @Test
    fun `option deleteBeforeReplace should be properly set`() = runBlocking {
        // when
        val optsDeleteBeforeReplaceTrue = opts {
            deleteBeforeReplace(true)
        }

        // then
        assertTrue(optsDeleteBeforeReplaceTrue.deleteBeforeReplace, "opts.deleteBeforeReplace")
    }

    @Test
    fun `option dependsOn should be properly set`() = runBlocking {
        // given
        val kotlinResource1 =
            mockKotlinSdkResource(KotlinResource::class, com.pulumi.resources.Resource::class)
        val kotlinResource2 =
            mockKotlinSdkResource(KotlinResource::class, com.pulumi.resources.Resource::class)
        val kotlinResource3 =
            mockKotlinSdkResource(KotlinResource::class, com.pulumi.resources.Resource::class)
        val kotlinSdkResources = listOf(kotlinResource1, kotlinResource2, kotlinResource3)
        val outputListOfKotlinSdkResources = Output.of(kotlinSdkResources)
        val expectedResourcesAmount = 3

        // when
        val optsCreatedWithVarargs = opts {
            dependsOn(kotlinResource1, kotlinResource2, kotlinResource3)
        }
        val optsCreatedWithList = opts {
            dependsOn(kotlinSdkResources)
        }
        val optsCreatedWithOutputList = opts {
            dependsOn(outputListOfKotlinSdkResources)
        }

        // then
        val actualValueFromVarargs = extractOutputValue(optsCreatedWithVarargs.dependsOn)
        val actualValueFromList = extractOutputValue(optsCreatedWithList.dependsOn)
        val actualValueFromOutputList = extractOutputValue(optsCreatedWithOutputList.dependsOn)

        assertAll(
            { assertEquals(expectedResourcesAmount, actualValueFromVarargs?.size) },
            { assertEquals(expectedResourcesAmount, actualValueFromList?.size) },
            { assertEquals(expectedResourcesAmount, actualValueFromOutputList?.size) },
        )
    }

    @Test
    fun `option id should be properly set`() = runBlocking {
        // given
        val id = "id"
        val outputId = Output.of("id")

        // when
        val optsCreatedWithArg = opts {
            id(id)
        }
        val optsCreatedWithOutputArg = opts {
            id(outputId)
        }

        // then
        assertAll(
            assertOutputPropertyEqual(optsCreatedWithArg, id, "opts.id") { it.id },
            assertOutputPropertyEqual(optsCreatedWithOutputArg, "id", "opts.id") { it.id },
        )
    }

    @Test
    fun `option ignoreChanges should be properly set`() = runBlocking {
        // given
        val ignoredChange1 = "resource-name-1"
        val ignoredChange2 = "resource-name-2"
        val ignoredChanges = listOf(ignoredChange1, ignoredChange2)

        // when
        val optsCreatedWithVarargs = opts {
            ignoreChanges(
                ignoredChange1,
                ignoredChange2,
            )
        }
        val optsCreatedWithList = opts {
            ignoreChanges(ignoredChanges)
        }

        assertAll(
            { assertContentEquals(ignoredChanges, optsCreatedWithVarargs.ignoreChanges) },
            { assertContentEquals(ignoredChanges, optsCreatedWithList.ignoreChanges) },
        )
    }

    @Test
    fun `option importId should be properly set`() = runBlocking {
        // given
        val importId = "import-id"

        // when
        val opts = opts {
            importId(importId)
        }

        // then
        assertEquals(opts.importId, importId, "opts.importId")
    }

    @Test
    fun `option parent should be properly set`() = runBlocking {
        // given
        val parentKotlinResource =
            mockKotlinSdkResource(KotlinResource::class, com.pulumi.resources.Resource::class)

        // when
        val opts = opts {
            parent(parentKotlinResource)
        }

        // then
        assertEquals(opts.parent, parentKotlinResource, "opts.parent")
    }

    @Test
    fun `option pluginDownloadURL should be properly set`() = runBlocking {
        // given
        val pluginDownloadURL = "https://example.org"

        // when
        val opts = opts {
            pluginDownloadURL(pluginDownloadURL)
        }

        // then
        assertEquals(opts.pluginDownloadURL, pluginDownloadURL, "opts.pluginDownloadURL")
    }

    @Test
    fun `option protect should be properly set`() = runBlocking {
        // when
        val optsProtectTrue = opts {
            protect(true)
        }
        val optsProtectFalse = opts {
        }

        // then
        assertAll(
            { assertTrue { optsProtectTrue.protect } },
            { assertFalse { optsProtectFalse.protect } },
        )
    }

    @Test
    fun `option provider should be properly set`() = runBlocking {
        // given
        val providerResource =
            mockKotlinSdkResource(KotlinProviderResource::class, com.pulumi.resources.ProviderResource::class)

        // when
        val opts = opts {
            provider(providerResource)
        }

        // then
        assertEquals(opts.provider, providerResource, "opts.provider")
    }

    @Test
    fun `option replaceOnChanges should be properly set`() = runBlocking {
        // given
        val replaceOnChange1 = "resource-to-replace-1"
        val replaceOnChange2 = "resource-to-replace-2"
        val replaceOnChangeList = listOf(replaceOnChange1, replaceOnChange2)

        // when
        val optsCreatedWithVarargs = opts {
            replaceOnChanges(replaceOnChange1, replaceOnChange2)
        }
        val optsCreatedWithList = opts {
            replaceOnChanges(replaceOnChangeList)
        }

        // then
        assertAll(
            {
                assertContentEquals(
                    replaceOnChangeList,
                    optsCreatedWithVarargs.replaceOnChanges,
                    "opts.replaceOnChanges",
                )
            },
            {
                assertContentEquals(
                    replaceOnChangeList,
                    optsCreatedWithList.replaceOnChanges,
                    "opts.replaceOnChanges",
                )
            },
        )
    }

    @Test
    fun `option resourceTransformations should be properly set`() = runBlocking {
        // given
        val javaResourceMock = mockk<com.pulumi.resources.Resource>()
        val javaResourceArgsMock = mockk<com.pulumi.resources.ResourceArgs>()
        val javaResourceOptionsMock = mockk<com.pulumi.resources.ResourceOptions>()
        val javaArgs = com.pulumi.resources.ResourceTransformation.Args(
            javaResourceMock,
            javaResourceArgsMock,
            javaResourceOptionsMock,
        )

        val resourceTransformation = ResourceTransformation { transformation ->
            transformationResult {
                args(transformation.args())
                options(transformation.options())
            }
        }
        val resourceTransformationsList = listOf(resourceTransformation)

        // when
        val optsCreatedWithVarargs = opts {
            resourceTransformations(resourceTransformation)
        }
        val optsCreatedWithList = opts {
            resourceTransformations(resourceTransformationsList)
        }

        // then
        val transformationCreatedWithVarargsApplicationResult = optsCreatedWithVarargs.resourceTransformations
            ?.map { it.apply(javaArgs) }
        val transformationCreatedWithListApplicationResult = optsCreatedWithList.resourceTransformations
            ?.map { it.apply(javaArgs) }

        assertAll(
            { assertEquals(1, transformationCreatedWithVarargsApplicationResult!!.size) },
            { assertEquals(1, transformationCreatedWithListApplicationResult!!.size) },

            {
                assertTrue(
                    transformationCreatedWithVarargsApplicationResult!!
                        .map { it!!.args() == javaResourceArgsMock && it.options() == javaResourceOptionsMock }
                        .reduce { acc, b -> acc && b },
                )
            },
            {
                assertTrue(
                    transformationCreatedWithListApplicationResult!!
                        .map { it!!.args() == javaResourceArgsMock && it.options() == javaResourceOptionsMock }
                        .reduce { acc, b -> acc && b },
                )
            },
        )
    }

    @Test
    fun `option retainOnDelete should be properly set`() = runBlocking {
        // when
        val optsRetainOnDeleteTrue = opts {
            retainOnDelete(true)
        }

        // then
        assertAll(
            { assertTrue(optsRetainOnDeleteTrue.retainOnDelete, "opts.retainOnDelete") },
        )
    }

    @Test
    fun `option urn should be properly set`() = runBlocking {
        // given
        val urn = "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket"

        // when
        val opts = opts {
            urn(urn)
        }

        // then
        assertEquals(opts.urn, urn, "opts.urn")
    }

    @Test
    fun `option version should be properly set`() = runBlocking {
        // given
        val version = "1.0.0"

        // when
        val opts = opts {
            version(version)
        }

        // then
        assertEquals(opts.version, version, "opts.version")
    }

    @Test
    fun `options should be properly merged to existing options`() = runBlocking {
        // given
        val username = "username"
        val password = "password"
        val oldResourceName = "old-resource-name"
        val duration = 100.toDuration(DurationUnit.MILLISECONDS)
        val oldProjectName = "old-project-name"
        val givenOptions = opts {
            additionalSecretOutputs(username)
            aliases(
                {
                    name(oldResourceName)
                },
            )
            customTimeouts {
                update(duration)
            }
        }

        // when
        val currentOptions = opts {
            additionalSecretOutputs(password)
            aliases(
                {
                    project(oldProjectName)
                },
            )
            customTimeouts {
                create(duration)
            }
            mergeWith(givenOptions)
        }

        // then
        val oldNamesExtractedFromAliases = currentOptions.aliases!!
            .map { extractOutputValue(it) }
            // config above assumes there are only aliases with those values each, hence such extraction
            .map { it?.name ?: it?.project }
            .map { extractOutputValue(it) }

        assertAll(
            { assertContentEquals(listOf(username, password), currentOptions.additionalSecretOutputs) },
            { assertContentEquals(listOf(oldResourceName, oldProjectName), oldNamesExtractedFromAliases) },
            { assertNull(currentOptions.customTimeouts!!.update) },
            { assertEquals(duration, currentOptions.customTimeouts!!.create) },
        )
    }

    @Test
    fun `empty options should contain only nulls or type defaults`() = runBlocking {
        // when
        val opts = opts {
        }

        // then
        assertAll(
            { assertTrue(opts.additionalSecretOutputs!!.isEmpty(), "opts.additionalSecretOutputs") },
            { assertTrue(opts.aliases!!.isEmpty(), "opts.aliases") },
            { assertNull(opts.customTimeouts, "opts.customTimeouts") },
            { assertFalse(opts.deleteBeforeReplace, "opts.deleteBeforeReplace") },
            { assertTrue(extractOutputValue(opts.dependsOn)!!.isEmpty(), "opts.dependsOn") },
            { assertNull(opts.id, "opts.id") },
            { assertTrue(opts.ignoreChanges!!.isEmpty(), "opts.ignoreChanges") },
            { assertNull(opts.importId, "opts.importId") },
            { assertNull(opts.pluginDownloadURL, "opts.pluginDownloadURL") },
            { assertNull(opts.parent, "opts.parent") },
            { assertFalse(opts.protect, "opts.protect") },
            { assertNull(opts.provider, "opts.provider") },
            { assertTrue(opts.replaceOnChanges!!.isEmpty(), "opts.replaceOnChanges") },
            { assertTrue(opts.resourceTransformations!!.isEmpty(), "opts.resourceTransformations") },
            { assertFalse(opts.retainOnDelete, "opts.retainOnDelete") },
            { assertNull(opts.urn, "opts.urn") },
            { assertNull(opts.version, "opts.version") },
        )
    }

    private fun <T : KotlinResource, R : com.pulumi.resources.Resource> mockKotlinSdkResource(
        kotlinResourceToMock: KClass<T>,
        javaResourceEquivalentToMock: KClass<R>,
    ): T {
        val mockedKotlinSdkResource = mockkClass(kotlinResourceToMock)
        val mockedJavaResource = mockkClass(javaResourceEquivalentToMock)

        every {
            mockedKotlinSdkResource.javaResource
        } returns mockedJavaResource

        mockkObject(GeneralResourceMapper)
        every {
            GeneralResourceMapper.tryMap(any<com.pulumi.resources.Resource>())
        } returns mockedKotlinSdkResource

        return mockedKotlinSdkResource
    }
}

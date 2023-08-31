package com.pulumi.kotlin.options

import com.pulumi.core.Output
import com.pulumi.kotlin.GlobalResourceMapper
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.extractOutputValue
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
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.reflect.KClass
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.milliseconds
import com.pulumi.resources.ProviderResource as JavaProviderResource
import com.pulumi.resources.Resource as JavaResource
import com.pulumi.resources.ResourceArgs as JavaResourceArgs
import com.pulumi.resources.ResourceOptions as JavaResourceOptions
import com.pulumi.resources.ResourceTransformation.Args as JavaResourceTransformationArgs
import com.pulumi.resources.ResourceTransformation.Result as JavaResourceTransformationResult

internal class CustomResourceOptionsTest {

    @AfterEach
    fun cleanUpAfterTests() {
        unmockkAll()
    }

    @Test
    fun `option additionalSecretOutputs should be properly set, when list given as input`() = runBlocking {
        // when
        val optsCreatedWithList = opts {
            additionalSecretOutputs(listOf("username", "password"))
        }

        // then
        assertAll(
            {
                assertContentEquals(
                    listOf("username", "password"),
                    optsCreatedWithList.additionalSecretOutputs,
                    "additionalSecretOutputs created with list arg",
                )
            },
        )
    }

    @Test
    fun `option additionalSecretOutputs should be properly set, when varargs given as input`() = runBlocking {
        // when
        val opts = opts {
            additionalSecretOutputs("username", "password")
        }

        // then
        assertAll(
            {
                assertContentEquals(
                    listOf("username", "password"),
                    opts.additionalSecretOutputs,
                    "additionalSecretOutputs created with list arg",
                )
            },
        )
    }

    @Test
    fun `option aliases should be properly set, when created with type-safe builder`() = runBlocking {
        // given
        val alias = alias {
            name("old-resource-name")
        }

        // when
        val opts = opts {
            aliases(
                alias {
                    name("old-resource-name")
                },
                withUrn("urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket"),
                noParent(),
                alias,
            )
        }

        // then
        val actualAliasesFromOpts = opts.aliases?.map { extractOutputValue(it) }.orEmpty()
        val actualAliasesWithUrn =
            actualAliasesFromOpts.filter {
                it?.urn == "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket"
            }
        val actualAliasesWithoutParent = actualAliasesFromOpts.filter { it?.noParent!! }
        val actualAliasesWithName = actualAliasesFromOpts.filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertAll(
            { assertEquals(4, actualAliasesFromOpts.size, "total aliases amount") },
            { assertEquals(1, actualAliasesWithUrn.size, "aliases with given urn amount") },
            { assertEquals(1, actualAliasesWithoutParent.size, "aliases without parent amount") },
            { assertEquals(2, actualAliasesWithName.size, "aliases with given name amount") },
        )
    }

    @Test
    fun `option aliases should be properly set, when created with varargs`() = runBlocking {
        // when
        val opts = opts {
            aliases(
                alias {
                    name("old-resource-name")
                },
                alias {
                    name("old-resource-name")
                },
            )
        }

        // then
        val actualAliasesWithName = opts.aliases
            ?.map { extractOutputValue(it) }
            .orEmpty()
            .filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertEquals(2, actualAliasesWithName.size, "total amount of aliases with given name")
    }

    @Test
    fun `option aliases should be properly set, when created with varargs of outputs`() = runBlocking {
        // when
        val opts = opts {
            aliases(
                Output.of(
                    alias {
                        name("old-resource-name")
                    },
                ),
                Output.of(
                    alias {
                        name("old-resource-name")
                    },
                ),
            )
        }

        // then
        val actualAliasesWithName = opts.aliases
            ?.map { extractOutputValue(it) }
            .orEmpty()
            .filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertEquals(2, actualAliasesWithName.size, "total amount of aliases with given name")
    }

    @Test
    fun `option aliases should be properly set, when created with list`() = runBlocking {
        // when
        val opts = opts {
            aliases(
                listOf(
                    Output.of(
                        alias {
                            name("old-resource-name")
                        },
                    ),
                    Output.of(
                        alias {
                            name("old-resource-name")
                        },
                    ),
                ),
            )
        }

        // then
        val actualAliasesWithName = opts.aliases
            ?.map { extractOutputValue(it) }
            .orEmpty()
            .filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertEquals(2, actualAliasesWithName.size, "total amount of aliases with given name")
    }

    @Test
    fun `option customTimeouts should be properly set, when created with type-safe builder`() = runBlocking {
        // when
        val opts = opts {
            customTimeouts {
                create(100.milliseconds)
                update(100.milliseconds)
                delete(100.milliseconds)
            }
        }

        // then
        assertAll(
            {
                assertEquals(
                    100.milliseconds,
                    opts.customTimeouts!!.create!!,
                    "custom timeouts - create",
                )
            },
            {
                assertEquals(
                    100.milliseconds,
                    opts.customTimeouts!!.update!!,
                    "custom timeouts - update",
                )
            },
            {
                assertEquals(
                    100.milliseconds,
                    opts.customTimeouts!!.delete!!,
                    "custom timeouts - delete",
                )
            },
        )
    }

    @Test
    fun `option customTimeouts should be properly set, when created with object`() = runBlocking {
        // given
        val customTimeouts = customTimeouts {
            create(100.milliseconds)
            update(100.milliseconds)
            delete(100.milliseconds)
        }

        // when
        val optsCreatedWithObject = opts {
            customTimeouts(customTimeouts)
        }

        // then
        assertAll(
            {
                assertEquals(
                    100.milliseconds,
                    optsCreatedWithObject.customTimeouts!!.create!!,
                    "custom timeouts - create",
                )
            },
            {
                assertEquals(
                    100.milliseconds,
                    optsCreatedWithObject.customTimeouts!!.update!!,
                    "custom timeouts - update",
                )
            },
            {
                assertEquals(
                    100.milliseconds,
                    optsCreatedWithObject.customTimeouts!!.delete!!,
                    "custom timeouts - delete",
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
        assertTrue(optsDeleteBeforeReplaceTrue.deleteBeforeReplace, "deleteBeforeReplace")
    }

    @Test
    fun `option dependsOn should be properly set with varargs`() = runBlocking {
        // given
        val kotlinResource1 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource2 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource3 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)

        // when
        val opts = opts {
            dependsOn(kotlinResource1, kotlinResource2, kotlinResource3)
        }

        // then
        val actualDependsOnList = extractOutputValue(opts.dependsOn).orEmpty()

        assertEquals(3, actualDependsOnList.size, "amount of dependencies")
    }

    @Test
    fun `option dependsOn should be properly set with list`() = runBlocking {
        // given
        val kotlinResource1 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource2 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource3 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResources = listOf(kotlinResource1, kotlinResource2, kotlinResource3)

        // when
        val opts = opts {
            dependsOn(kotlinResources)
        }

        // then
        val actualDependsOnList = extractOutputValue(opts.dependsOn).orEmpty()

        assertEquals(3, actualDependsOnList.size, "amount of dependencies")
    }

    @Test
    fun `option dependsOn should be properly set with output of list`() = runBlocking {
        // given
        val kotlinResource1 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource2 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource3 =
            mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResources = listOf(kotlinResource1, kotlinResource2, kotlinResource3)
        val outputListOfKotlinResources = Output.of(kotlinResources)

        // when
        val opts = opts {
            dependsOn(outputListOfKotlinResources)
        }

        // then
        val actualDependsOnList = extractOutputValue(opts.dependsOn).orEmpty()

        assertEquals(3, actualDependsOnList.size, "amount of dependencies")
    }

    @Test
    fun `option id should be properly set with string`() = runBlocking {
        // when
        val opts = opts {
            id("id")
        }

        // then
        val actualId = extractOutputValue(opts.id)

        assertEquals("id", actualId, "id")
    }

    @Test
    fun `option id should be properly set with output of string`() = runBlocking {
        // when
        val opts = opts {
            id(Output.of("id"))
        }

        // then
        val actualId = extractOutputValue(opts.id)

        assertEquals("id", actualId, "id")
    }

    @Test
    fun `option ignoreChanges should be properly set with varargs`() = runBlocking {
        // when
        val opts = opts {
            ignoreChanges(
                "resource-name-1",
                "resource-name-2",
            )
        }

        // then
        assertContentEquals(listOf("resource-name-1", "resource-name-2"), opts.ignoreChanges, "ignored changes")
    }

    @Test
    fun `option ignoreChanges should be properly set with list`() = runBlocking {
        // given
        val ignoredChanges = listOf("resource-name-1", "resource-name-2")

        // when
        val opts = opts {
            ignoreChanges(ignoredChanges)
        }

        // then
        assertContentEquals(ignoredChanges, opts.ignoreChanges, "ignored changes")
    }

    @Test
    fun `option importId should be properly set`() = runBlocking {
        // when
        val opts = opts {
            importId("import-id")
        }

        // then
        assertEquals("import-id", opts.importId, "importId")
    }

    @Test
    fun `option parent should be properly set`() = runBlocking {
        // given
        val parentKotlinResource =
            mockKotlinResource(KotlinResource::class, JavaResource::class)

        // when
        val opts = opts {
            parent(parentKotlinResource)
        }

        // then
        assertEquals(parentKotlinResource, opts.parent, "parent")
    }

    @Test
    fun `option pluginDownloadURL should be properly set`() = runBlocking {
        // when
        val opts = opts {
            pluginDownloadURL("https://example.org")
        }

        // then
        assertEquals("https://example.org", opts.pluginDownloadURL, "pluginDownloadURL")
    }

    @Test
    fun `option protect should be properly set`() = runBlocking {
        // when
        val opts = opts {
            protect(true)
        }

        // then
        assertTrue(opts.protect)
    }

    @Test
    fun `option provider should be properly set`() = runBlocking {
        // given
        val providerResource =
            mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

        // when
        val opts = opts {
            provider(providerResource)
        }

        // then
        assertEquals(providerResource, opts.provider, "provider")
    }

    @Test
    fun `option replaceOnChanges should be properly set with varargs`() = runBlocking {
        // given

        // when
        val opts = opts {
            replaceOnChanges("resource-to-replace-1", "resource-to-replace-2")
        }

        // then
        assertContentEquals(
            listOf("resource-to-replace-1", "resource-to-replace-2"),
            opts.replaceOnChanges,
            "replaceOnChanges",
        )
    }

    @Test
    fun `option replaceOnChanges should be properly set with list`() = runBlocking {
        // given
        val replaceOnChangeList = listOf("resource-to-replace-1", "resource-to-replace-2")

        // when
        val opts = opts {
            replaceOnChanges(replaceOnChangeList)
        }

        // then
        assertContentEquals(
            listOf("resource-to-replace-1", "resource-to-replace-2"),
            opts.replaceOnChanges,
            "replaceOnChanges",
        )
    }

    @Test
    fun `option resourceTransformations should be properly set with varargs`() = runBlocking {
        // given
        val javaResourceMock = mockk<JavaResource>()
        val javaResourceArgsMock = mockk<JavaResourceArgs>()
        val javaResourceOptionsMock = mockk<JavaResourceOptions>()
        val javaArgs = JavaResourceTransformationArgs(
            javaResourceMock,
            javaResourceArgsMock,
            javaResourceOptionsMock,
        )

        val expectedResourceTransformationResult =
            JavaResourceTransformationResult(javaResourceArgsMock, javaResourceOptionsMock)

        // when
        val opts = opts {
            resourceTransformations(
                { transformation ->
                    transformationResult {
                        args(transformation.args())
                        options(transformation.options())
                    }
                },
            )
        }

        // then
        assertResourceTransformationResultEquals(
            expectedResourceTransformationResult,
            opts.resourceTransformations.orEmpty(),
            javaArgs,
        )
    }

    @Test
    fun `option resourceTransformations should be properly set with list`() = runBlocking {
        // given
        val javaResourceMock = mockk<JavaResource>()
        val javaResourceArgsMock = mockk<JavaResourceArgs>()
        val javaResourceOptionsMock = mockk<JavaResourceOptions>()
        val javaArgs = JavaResourceTransformationArgs(
            javaResourceMock,
            javaResourceArgsMock,
            javaResourceOptionsMock,
        )

        val expectedResourceTransformationResult =
            JavaResourceTransformationResult(javaResourceArgsMock, javaResourceOptionsMock)

        val resourceTransformation = ResourceTransformation { transformation ->
            transformationResult {
                args(transformation.args())
                options(transformation.options())
            }
        }

        // when
        val opts = opts {
            resourceTransformations(listOf(resourceTransformation))
        }

        // then
        assertResourceTransformationResultEquals(
            expectedResourceTransformationResult,
            opts.resourceTransformations.orEmpty(),
            javaArgs,
        )
    }

    @Test
    fun `option retainOnDelete should be properly set`() = runBlocking {
        // when
        val opts = opts {
            retainOnDelete(true)
        }

        // then
        assertTrue(opts.retainOnDelete, "retainOnDelete")
    }

    @Test
    fun `option urn should be properly set`() = runBlocking {
        // when
        val opts = opts {
            urn("urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket")
        }

        // then
        assertEquals(
            "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket",
            opts.urn,
            "opts.urn",
        )
    }

    @Test
    fun `option version should be properly set`() = runBlocking {
        // when
        val opts = opts {
            version("1.0.0")
        }

        // then
        assertEquals("1.0.0", opts.version, "opts.version")
    }

    @Test
    fun `options should be properly merged to existing options`() = runBlocking {
        // given
        val username = "username"
        val password = "password"
        val oldResourceName = "old-resource-name"
        val duration = 100.milliseconds
        val oldProjectName = "old-project-name"

        val givenOpts = opts {
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
            mergeWith(givenOpts)
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
            { assertEquals(duration, currentOptions.customTimeouts!!.create!!) },
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

    private fun assertResourceTransformationResultEquals(
        expectedResult: JavaResourceTransformationResult,
        actualTransformations: List<ResourceTransformation>,
        transformationArgs: JavaResourceTransformationArgs,
    ) {
        if (actualTransformations.isEmpty()) {
            fail<JavaResourceTransformationResult>("Resource transformation results are empty!")
        }

        val actualToExpectedResult = actualTransformations
            .map { it.apply(transformationArgs) }
            // assert that after invocation of transformation the invocation results would be the same as expected,
            .map { { assertTrue { it!!.args() == expectedResult.args() && it.options() == expectedResult.options() } } }

        assertAll(actualToExpectedResult)
    }

    private fun <T : KotlinResource, R : JavaResource> mockKotlinResource(
        kotlinResourceToMock: KClass<T>,
        javaResourceEquivalentToMock: KClass<R>,
    ): T {
        val mockedKotlinResource = mockkClass(kotlinResourceToMock)
        val mockedJavaResource = mockkClass(javaResourceEquivalentToMock)

        every {
            mockedKotlinResource.underlyingJavaResource
        } returns mockedJavaResource

        mockkObject(GlobalResourceMapper)
        every {
            GlobalResourceMapper.tryMap(any<JavaResource>())
        } returns mockedKotlinResource

        return mockedKotlinResource
    }
}

package com.pulumi.kotlin.options

import com.pulumi.core.Output
import com.pulumi.kotlin.GlobalResourceMapper
import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.KotlinResource
import com.pulumi.kotlin.assertResourceTransformationResultEquals
import com.pulumi.kotlin.extractOutputValue
import com.pulumi.kotlin.mockKotlinResource
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.reflect.KSuspendFunction1
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.milliseconds
import com.pulumi.resources.ProviderResource as JavaProviderResource
import com.pulumi.resources.Resource as JavaResource
import com.pulumi.resources.ResourceArgs as JavaResourceArgs
import com.pulumi.resources.ResourceOptions as JavaResourceOptions
import com.pulumi.resources.ResourceTransformation.Args as JavaResourceTransformationArgs
import com.pulumi.resources.ResourceTransformation.Result as JavaResourceTransformationResult

@Suppress("unused", "UnusedPrivateMember")
internal class ResourceOptionsTest {

    @BeforeEach
    fun setUpMocking() {
        mockkObject(GlobalResourceMapper)
    }

    @AfterEach
    fun cleanUpAfterTests() {
        unmockkAll()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option aliases should be properly set, when created with type-safe builder`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val alias = alias {
            name("old-resource-name")
        }

        // when
        val resourceOptions = opts {
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
        val actualAliasesFromOpts = resourceOptions.aliases?.map { extractOutputValue(it) }.orEmpty()
        val actualAliasesWithUrn =
            actualAliasesFromOpts.filter {
                it?.urn == "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket"
            }
        val actualAliasesWithoutParent = actualAliasesFromOpts.filter { it?.noParent!! }
        val actualAliasesWithName =
            actualAliasesFromOpts.filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertAll(
            { assertEquals(4, actualAliasesFromOpts.size, "total aliases amount") },
            { assertEquals(1, actualAliasesWithUrn.size, "aliases with given urn amount") },
            { assertEquals(1, actualAliasesWithoutParent.size, "aliases without parent amount") },
            { assertEquals(2, actualAliasesWithName.size, "aliases with given name amount") },
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option aliases should be properly set, when created with varargs`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
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
        val actualAliasesWithName = resourceOptions.aliases
            ?.map { extractOutputValue(it) }
            .orEmpty()
            .filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertEquals(2, actualAliasesWithName.size, "total amount of aliases with given name")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option aliases should be properly set, when created with varargs of outputs`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
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
        val actualAliasesWithName = resourceOptions.aliases
            ?.map { extractOutputValue(it) }
            .orEmpty()
            .filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertEquals(2, actualAliasesWithName.size, "total amount of aliases with given name")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option aliases should be properly set, when created with list`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
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
        val actualAliasesWithName = resourceOptions.aliases
            ?.map { extractOutputValue(it) }
            .orEmpty()
            .filter { extractOutputValue(it?.name) == "old-resource-name" }

        assertEquals(2, actualAliasesWithName.size, "total amount of aliases with given name")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option customTimeouts should be properly set, when created with type-safe builder`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
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
                    resourceOptions.customTimeouts!!.create!!,
                    "custom timeouts - create",
                )
            },
            {
                assertEquals(
                    100.milliseconds,
                    resourceOptions.customTimeouts!!.update!!,
                    "custom timeouts - update",
                )
            },
            {
                assertEquals(
                    100.milliseconds,
                    resourceOptions.customTimeouts!!.delete!!,
                    "custom timeouts - delete",
                )
            },
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option customTimeouts should be properly set, when created with object`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option dependsOn should be properly set with varargs`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val kotlinResource1 = mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource2 = mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource3 = mockKotlinResource(KotlinResource::class, JavaResource::class)

        // when
        val resourceOptions = opts {
            dependsOn(kotlinResource1, kotlinResource2, kotlinResource3)
        }

        // then
        val actualDependsOnList = extractOutputValue(resourceOptions.dependsOn).orEmpty()

        assertEquals(3, actualDependsOnList.size, "number of dependencies")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option dependsOn should be properly set with list`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val kotlinResource1 = mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource2 = mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource3 = mockKotlinResource(KotlinResource::class, JavaResource::class)

        val kotlinResources = listOf(kotlinResource1, kotlinResource2, kotlinResource3)

        // when
        val resourceOptions = opts {
            dependsOn(kotlinResources)
        }

        // then
        val actualDependsOnList = extractOutputValue(resourceOptions.dependsOn).orEmpty()

        assertEquals(3, actualDependsOnList.size, "number of dependencies")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option dependsOn should be properly set with output of list`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val kotlinResource1 = mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource2 = mockKotlinResource(KotlinResource::class, JavaResource::class)
        val kotlinResource3 = mockKotlinResource(KotlinResource::class, JavaResource::class)

        val kotlinResources = listOf(kotlinResource1, kotlinResource2, kotlinResource3)
        val outputListOfKotlinResources = Output.of(kotlinResources)

        // when
        val resourceOptions = opts {
            dependsOn(outputListOfKotlinResources)
        }

        // then
        val actualDependsOnList = extractOutputValue(resourceOptions.dependsOn).orEmpty()

        assertEquals(3, actualDependsOnList.size, "number of dependencies")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option id should be properly set with string`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            id("id")
        }

        // then
        val actualId = extractOutputValue(resourceOptions.id)

        assertEquals("id", actualId, "id")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option id should be properly set with output of string`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            id(Output.of("id"))
        }

        // then
        val actualId = extractOutputValue(resourceOptions.id)

        assertEquals("id", actualId, "id")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option ignoreChanges should be properly set with varargs`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            ignoreChanges(
                "resource-name-1",
                "resource-name-2",
            )
        }

        // then
        assertContentEquals(
            listOf("resource-name-1", "resource-name-2"),
            resourceOptions.ignoreChanges,
            "ignored changes",
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option ignoreChanges should be properly set with list`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val ignoredChanges = listOf("resource-name-1", "resource-name-2")

        // when
        val resourceOptions = opts {
            ignoreChanges(ignoredChanges)
        }

        // then
        assertContentEquals(ignoredChanges, resourceOptions.ignoreChanges, "ignored changes")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option parent should be properly set`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val parentKotlinResource =
            mockKotlinResource(KotlinResource::class, JavaResource::class)

        // when
        val resourceOptions = opts {
            parent(parentKotlinResource)
        }

        // then
        assertEquals(parentKotlinResource, resourceOptions.parent, "parent")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option pluginDownloadURL should be properly set`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            pluginDownloadURL("https://example.org")
        }

        // then
        assertEquals("https://example.org", resourceOptions.pluginDownloadURL, "pluginDownloadURL")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option protect should be properly set`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            protect(true)
        }

        // then
        assertTrue(resourceOptions.protect)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option provider should be properly set`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val providerResource = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

        // when
        val resourceOptions = opts {
            provider(providerResource)
        }

        // then
        assertEquals(providerResource, resourceOptions.provider, "provider")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option replaceOnChanges should be properly set with varargs`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            replaceOnChanges("resource-to-replace-1", "resource-to-replace-2")
        }

        // then
        assertContentEquals(
            listOf("resource-to-replace-1", "resource-to-replace-2"),
            resourceOptions.replaceOnChanges,
            "replaceOnChanges",
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option replaceOnChanges should be properly set with list`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // given
        val replaceOnChangeList = listOf("resource-to-replace-1", "resource-to-replace-2")

        // when
        val resourceOptions = opts {
            replaceOnChanges(replaceOnChangeList)
        }

        // then
        assertContentEquals(
            listOf("resource-to-replace-1", "resource-to-replace-2"),
            resourceOptions.replaceOnChanges,
            "replaceOnChanges",
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option resourceTransformations should be properly set with varargs`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
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
        val resourceOptions = opts {
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
            resourceOptions.resourceTransformations.orEmpty(),
            javaArgs,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option resourceTransformations should be properly set with list`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
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
        val resourceOptions = opts {
            resourceTransformations(listOf(resourceTransformation))
        }

        // then
        assertResourceTransformationResultEquals(
            expectedResourceTransformationResult,
            resourceOptions.resourceTransformations.orEmpty(),
            javaArgs,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option retainOnDelete should be properly set`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            retainOnDelete(true)
        }

        // then
        assertTrue(resourceOptions.retainOnDelete, "retainOnDelete")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option urn should be properly set`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            urn("urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket")
        }

        // then
        assertEquals(
            "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket",
            resourceOptions.urn,
            "opts.urn",
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optsFunctions")
    fun `option version should be properly set`(
        description: String,
        opts: KSuspendFunction1<suspend ResourceOptionsBuilder<*>.() -> Unit, ResourceOptions<*>>,
    ) = runBlocking {
        // when
        val resourceOptions = opts {
            version("1.0.0")
        }

        // then
        assertEquals("1.0.0", resourceOptions.version, "opts.version")
    }

    @Test
    fun `option additionalSecretOutputs should be properly set, when list given as input (CustomResourceOptions)`() =
        runBlocking {
            // when
            val optsCreatedWithList = CustomResourceOptions.opts {
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
    fun `option additionalSecretOutputs should be properly set, when varargs given as input (CustomResourceOptions)`() =
        runBlocking {
            // when
            val opts = CustomResourceOptions.opts {
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
    fun `option deleteBeforeReplace should be properly set (CustomResourceOptions)`() = runBlocking {
        // when
        val optsDeleteBeforeReplaceTrue = CustomResourceOptions.opts {
            deleteBeforeReplace(true)
        }

        // then
        assertTrue(optsDeleteBeforeReplaceTrue.deleteBeforeReplace, "deleteBeforeReplace")
    }

    @Test
    fun `option importId should be properly set (CustomResourceOptions)`() = runBlocking {
        // when
        val opts = CustomResourceOptions.opts {
            importId("import-id")
        }

        // then
        assertEquals("import-id", opts.importId, "importId")
    }

    @Test
    fun `option providers should be properly set, when list given as input (ComponentResourceOptions)`() = runBlocking {
        // given
        val mockedProvider1 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)
        val mockedProvider2 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

        // when
        val optsCreatedWithList = ComponentResourceOptions.opts {
            providers(listOf(mockedProvider1, mockedProvider2))
        }

        // then
        assertAll(
            {
                assertContentEquals(
                    listOf(mockedProvider1, mockedProvider2).map { it.underlyingJavaResource },
                    optsCreatedWithList.providers,
                    "providers created with list arg",
                )
            },
        )
    }

    @Test
    fun `option providers should be properly set, when varargs given as input (ComponentResourceOptions)`() =
        runBlocking {
            // given
            val mockedProvider1 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)
            val mockedProvider2 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

            // when
            val opts = ComponentResourceOptions.opts {
                providers(mockedProvider1, mockedProvider2)
            }

            // then
            assertAll(
                {
                    assertContentEquals(
                        listOf(mockedProvider1, mockedProvider2).map { it.underlyingJavaResource },
                        opts.providers,
                        "providers created with list arg",
                    )
                },
            )
        }

    @Test
    fun `options should be properly merged to existing options (ComponentResourceOptions)`() = runBlocking {
        // given
        val mockedProvider1 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)
        val mockedProvider2 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

        val oldResourceName = "old-resource-name"
        val duration = 100.milliseconds
        val oldProjectName = "old-project-name"

        val givenOpts = ComponentResourceOptions.opts {
            providers(mockedProvider1)
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
        val currentOptions = ComponentResourceOptions.opts {
            providers(mockedProvider2)
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
        val oldNamesExtractedFromAliases = extractOldNamesFromAlias(currentOptions)

        assertAll(
            {
                assertContentEquals(
                    listOf(mockedProvider1, mockedProvider2).map { it.underlyingJavaResource },
                    currentOptions.providers,
                )
            },
            { assertContentEquals(listOf(oldResourceName, oldProjectName), oldNamesExtractedFromAliases) },
            { assertNull(currentOptions.customTimeouts!!.update) },
            { assertEquals(duration, currentOptions.customTimeouts!!.create) },
        )
    }

    @Test
    fun `options should be properly merged to existing options (CustomResourceOptions)`() = runBlocking {
        // given
        val username = "username"
        val password = "password"
        val oldResourceName = "old-resource-name"
        val duration = 100.milliseconds
        val oldProjectName = "old-project-name"

        val givenOpts = CustomResourceOptions.opts {
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

        val currentOptions = CustomResourceOptions.opts {
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
        // when

        // then
        val oldNamesExtractedFromAliases = extractOldNamesFromAlias(currentOptions)

        assertAll(
            { assertContentEquals(listOf(username, password), currentOptions.additionalSecretOutputs) },
            { assertContentEquals(listOf(oldResourceName, oldProjectName), oldNamesExtractedFromAliases) },
            { assertNull(currentOptions.customTimeouts!!.update) },
            { assertEquals(duration, currentOptions.customTimeouts!!.create!!) },
        )
    }

    @Test
    fun `empty options should contain only nulls or type defaults (CustomResourceOptions)`() = runBlocking {
        // when
        val opts = CustomResourceOptions.opts {
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

    @Test
    fun `empty options should contain only nulls or type defaults (ComponentResourceOptions)`() = runBlocking {
        // when
        val opts = ComponentResourceOptions.opts {
        }

        // then
        assertAll(
            { assertTrue(opts.aliases!!.isEmpty(), "opts.aliases") },
            { assertNull(opts.customTimeouts, "opts.customTimeouts") },
            { assertTrue(extractOutputValue(opts.dependsOn)!!.isEmpty(), "opts.dependsOn") },
            { assertNull(opts.id, "opts.id") },
            { assertTrue(opts.ignoreChanges!!.isEmpty(), "opts.ignoreChanges") },
            { assertNull(opts.pluginDownloadURL, "opts.pluginDownloadURL") },
            { assertNull(opts.parent, "opts.parent") },
            { assertFalse(opts.protect, "opts.protect") },
            { assertNull(opts.provider, "opts.provider") },
            { assertTrue(opts.providers.isNullOrEmpty(), "opts.providers") },
            { assertTrue(opts.replaceOnChanges!!.isEmpty(), "opts.replaceOnChanges") },
            { assertTrue(opts.resourceTransformations!!.isEmpty(), "opts.resourceTransformations") },
            { assertFalse(opts.retainOnDelete, "opts.retainOnDelete") },
            { assertNull(opts.urn, "opts.urn") },
            { assertNull(opts.version, "opts.version") },
        )
    }

    private fun <T : ResourceOptions<*>> extractOldNamesFromAlias(currentOptions: T): List<String?> {
        return currentOptions.aliases!!
            .map { extractOutputValue(it) }
            // the config above assumes there are only aliases with those values each, hence such extraction
            .map { it?.name ?: it?.project }
            .map { extractOutputValue(it) }
    }

    companion object {
        @JvmStatic
        fun optsFunctions(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("CustomResourceOptions", CustomResourceOptions::opts),
                Arguments.of("ComponentResourceOptions", ComponentResourceOptions::opts),
            )
        }
    }
}

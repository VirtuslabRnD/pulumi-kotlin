package com.pulumi.kotlin.options

import com.pulumi.kotlin.KotlinProviderResource
import com.pulumi.kotlin.extractOutputValue
import com.pulumi.kotlin.mockKotlinResource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.milliseconds
import com.pulumi.resources.ProviderResource as JavaProviderResource

internal class ComponentResourceOptionsTest :
    ResourceOptionsTest<ComponentResourceOptions, ComponentResourceOptionsBuilder>() {

    override suspend fun opts(block: suspend ComponentResourceOptionsBuilder.() -> Unit): ComponentResourceOptions {
        return ComponentResourceOptions.opts(block)
    }

    @Test
    fun `option providers should be properly set, when list given as input`() = runBlocking {
        // given
        val mockedProvider1 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)
        val mockedProvider2 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

        // when
        val optsCreatedWithList = opts {
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
    fun `option providers should be properly set, when varargs given as input`() = runBlocking {
        // given
        val mockedProvider1 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)
        val mockedProvider2 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

        // when
        val opts = opts {
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
    override fun `options should be properly merged to existing options`() = runBlocking {
        // given
        val mockedProvider1 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)
        val mockedProvider2 = mockKotlinResource(KotlinProviderResource::class, JavaProviderResource::class)

        val oldResourceName = "old-resource-name"
        val duration = 100.milliseconds
        val oldProjectName = "old-project-name"

        val givenOpts = opts {
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
        val currentOptions = opts {
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
        val oldNamesExtractedFromAliases = currentOptions.aliases!!
            .map { extractOutputValue(it) }
            // config above assumes there are only aliases with those values each, hence such extraction
            .map { it?.name ?: it?.project }
            .map { extractOutputValue(it) }

        assertAll(
            {
                assertContentEquals(
                    listOf(mockedProvider1, mockedProvider2).map { it.underlyingJavaResource },
                    currentOptions.providers,
                )
            },
            { assertContentEquals(listOf(oldResourceName, oldProjectName), oldNamesExtractedFromAliases) },
            { assertNull(currentOptions.customTimeouts!!.update) },
            { assertEquals(duration, currentOptions.customTimeouts!!.create!!) },
        )
    }

    @Test
    override fun `empty options should contain only nulls or type defaults`() = runBlocking {
        // when
        val opts = opts {
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
}

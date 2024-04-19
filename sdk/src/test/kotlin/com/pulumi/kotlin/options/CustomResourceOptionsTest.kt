package com.pulumi.kotlin.options

import com.pulumi.kotlin.extractOutputValue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.milliseconds

internal class CustomResourceOptionsTest : ResourceOptionsTest<CustomResourceOptions, CustomResourceOptionsBuilder>() {

    override suspend fun opts(block: suspend CustomResourceOptionsBuilder.() -> Unit): CustomResourceOptions {
        return CustomResourceOptions.opts(block)
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
    fun `option deleteBeforeReplace should be properly set`() = runBlocking {
        // when
        val optsDeleteBeforeReplaceTrue = opts {
            deleteBeforeReplace(true)
        }

        // then
        assertTrue(optsDeleteBeforeReplaceTrue.deleteBeforeReplace, "deleteBeforeReplace")
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
    override fun `options should be properly merged to existing options`() = runBlocking {
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
    override fun `empty options should contain only nulls or type defaults`() = runBlocking {
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
}

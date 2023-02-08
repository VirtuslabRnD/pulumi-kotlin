package org.virtuslab.pulumikotlin.codegen.sdk.opts

import com.pulumi.core.Output
import com.pulumi.kotlin.options.alias
import com.pulumi.kotlin.options.noParent
import com.pulumi.kotlin.options.withUrn
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.virtuslab.pulumikotlin.extractOutputValue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// FIXME java builder methods are probably bugged, they verify nullstate of incorrect fields,
//  hence not all methods in type-safe builders could be tested,
//  complete these tests, when this case is solved
//  @see  https://github.com/VirtuslabRnD/jvm-lab/issues/54
internal class AliasTest {

    @Test
    fun `alias should be properly instantiated with use of strings in type-safe builder`() = runBlocking {
        // when
        val alias = alias {
            name("old-resource-name")
            type("custom-resource")
            stack("production")
            project("acmecorp")
        }

        // then
        val actualAliasName = extractOutputValue(alias.name)
        val actualAliasType = extractOutputValue(alias.type)
        val actualAliasStack = extractOutputValue(alias.stack)
        val actualAliasProject = extractOutputValue(alias.project)

        assertAll(
            { assertEquals("old-resource-name", actualAliasName, message = "old-resource-name") },
            { assertEquals("custom-resource", actualAliasType, message = "custom-resource") },
            { assertEquals("production", actualAliasStack, message = "production") },
            { assertEquals("acmecorp", actualAliasProject, message = "acmecorp") },
        )
    }

    @Test
    fun `alias should be properly instantiated with use of outputs of strings in type-safe builder`() = runBlocking {
        // when
        val alias = alias {
            name(Output.of("old-resource-name"))
            type(Output.of("custom-resource"))
            stack(Output.of("production"))
            project(Output.of("acmecorp"))
        }

        // then
        val actualAliasName = extractOutputValue(alias.name)
        val actualAliasType = extractOutputValue(alias.type)
        val actualAliasStack = extractOutputValue(alias.stack)
        val actualAliasProject = extractOutputValue(alias.project)

        assertAll(
            { assertEquals("old-resource-name", actualAliasName, message = "old-resource-name") },
            { assertEquals("custom-resource", actualAliasType, message = "custom-resource") },
            { assertEquals("production", actualAliasStack, message = "production") },
            { assertEquals("acmecorp", actualAliasProject, message = "acmecorp") },
        )
    }

    @Test
    fun `alias should be created with method withUrn`() = runBlocking {
        // when
        val alias = withUrn("urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket")

        // then
        assertAll(
            {
                assertEquals(
                    "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket",
                    alias.urn,
                    "urn",
                )
            },
            { assertNull(alias.name, "name") },
            { assertNull(alias.type, "type") },
            { assertNull(alias.project, "project") },
            { assertNull(alias.stack, "stack") },
            { assertNull(alias.parent, "parent") },
            { assertNull(alias.parentUrn, "parentUrn") },
            { assertFalse(alias.noParent, "noParent") },
        )
    }

    @Test
    fun `alias should be created with method noParent`() = runBlocking {
        // when
        val alias = noParent()

        // then
        assertAll(
            { assertNull(alias.urn, "urn") },
            { assertNull(alias.name, "name") },
            { assertNull(alias.type, "type") },
            { assertNull(alias.project, "project") },
            { assertNull(alias.stack, "stack") },
            { assertNull(alias.parent, "parent") },
            { assertNull(alias.parentUrn, "parentUrn") },
            { assertTrue(alias.noParent, "noParent") },
        )
    }
}

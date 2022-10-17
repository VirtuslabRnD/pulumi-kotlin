package com.virtuslab.pulumikotlin.codegen.sdk.opts

import com.pulumi.core.Output
import com.pulumi.kotlin.options.alias
import com.pulumi.kotlin.options.noParent
import com.pulumi.kotlin.options.withUrn
import com.virtuslab.pulumikotlin.assertOutputPropertyEqual
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ALIAS_URN = "urn:pulumi:production::acmecorp::custom:resources:Resource:s3/bucket:Bucket::my-bucket"
private const val ALIAS_NAME = "old-resource-name"
private const val ALIAS_TYPE = "custom-resource"
private const val ALIAS_STACK = "production"
private const val ALIAS_PROJECT = " acmecorp"

// FIXME java builder methods are probably bugged, they verify nullstate of incorrect fields,
//  hence not all methods in type-safe builders could be tested,
//  complete these tests, when this case is solved
internal class AliasTest {

    @Test
    fun `alias should be created with builder`() {
        runBlocking {
            // given
            val outputAliasName = Output.of(ALIAS_NAME)
            val outputAliasType = Output.of(ALIAS_TYPE)
            val outputAliasStack = Output.of(ALIAS_STACK)
            val outputAliasProject = Output.of(ALIAS_PROJECT)

            // when
            val aliasCreatedWithStrings = alias {
                name(ALIAS_NAME)
                type(ALIAS_TYPE)
                stack(ALIAS_STACK)
                project(ALIAS_PROJECT)
            }

            val aliasCreatedWithOutputs = alias {
                name(outputAliasName)
                type(outputAliasType)
                stack(outputAliasStack)
                project(outputAliasProject)
            }

            // then
            assertAll(
                assertOutputPropertyEqual(aliasCreatedWithStrings, ALIAS_NAME, "name") { it.name },
                assertOutputPropertyEqual(aliasCreatedWithStrings, ALIAS_TYPE, "type") { it.type },
                assertOutputPropertyEqual(aliasCreatedWithStrings, ALIAS_STACK, "stack") { it.stack },
                assertOutputPropertyEqual(aliasCreatedWithStrings, ALIAS_PROJECT, "project") { it.project },

                assertOutputPropertyEqual(aliasCreatedWithOutputs, ALIAS_NAME, "name") { it.name },
                assertOutputPropertyEqual(aliasCreatedWithOutputs, ALIAS_TYPE, "type") { it.type },
                assertOutputPropertyEqual(aliasCreatedWithOutputs, ALIAS_STACK, "stack") { it.stack },
                assertOutputPropertyEqual(aliasCreatedWithOutputs, ALIAS_PROJECT, "project") { it.project },
            )
        }
    }

    @Test
    fun `alias should be created with method withUrn`() {
        runBlocking {
            // when
            val alias = withUrn(ALIAS_URN)

            // then
            assertAll(
                { assertEquals(alias.urn, ALIAS_URN, "urn") },
                { assertNull(alias.name, "name") },
                { assertNull(alias.type, "type") },
                { assertNull(alias.project, "project") },
                { assertNull(alias.stack, "stack") },
                { assertNull(alias.parent, "parent") },
                { assertNull(alias.parentUrn, "parentUrn") },
                { assertFalse(alias.noParent, "noParent") },
            )
        }
    }

    @Test
    fun `alias should be created with method noParent`() {
        runBlocking {
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
}

package com.virtuslab.pulumikotlin.scripts

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

internal class ComputeSchemaSubsetScriptTest {

    @Test
    fun `should find subset when using type (that is referenced by some resource)`() {
        val outputSchema = runComputeSchemaSubsetScript(
            schemaPath = resolvedAwsClassicSchemaPath(),
            name = "aws:accessanalyzer/ArchiveRuleFilter:ArchiveRuleFilter",
            context = "type",
        )

        val decodedOutputSchema = Json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf(
                "aws:accessanalyzer/ArchiveRuleFilter:ArchiveRuleFilter",
            ),
            resources = setOf(
                "aws:accessanalyzer/archiveRule:ArchiveRule",
            ),
        )
    }

    @Test
    fun `should find subset when using resource (that references some types)`() {
        val outputSchema = runComputeSchemaSubsetScript(
            schemaPath = resolvedAwsClassicSchemaPath(),
            name = "aws:lambda/function:Function",
            context = "resource",
        )

        val decodedOutputSchema = Json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf(
                "aws:lambda/Runtime:Runtime",
                "aws:lambda/FunctionDeadLetterConfig:FunctionDeadLetterConfig",
                "aws:lambda/FunctionEnvironment:FunctionEnvironment",
                "aws:lambda/FunctionEphemeralStorage:FunctionEphemeralStorage",
                "aws:lambda/FunctionFileSystemConfig:FunctionFileSystemConfig",
                "aws:lambda/FunctionImageConfig:FunctionImageConfig",
                "aws:lambda/FunctionTracingConfig:FunctionTracingConfig",
                "aws:lambda/FunctionVpcConfig:FunctionVpcConfig",
            ),
            resources = setOf("aws:lambda/function:Function"),
        )
    }

    @Test
    fun `should find subset when using function (that references some types)`() {
        val outputSchema = runComputeSchemaSubsetScript(
            schemaPath = resolvedAwsClassicSchemaPath(),
            name = "aws:fsx/getOpenZfsSnapshot:getOpenZfsSnapshot",
            context = "function",
        )

        val decodedOutputSchema = Json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter"),
            functions = setOf("aws:fsx/getOpenZfsSnapshot:getOpenZfsSnapshot"),
        )
    }

    @Test
    fun `should work when using type (that is referenced by some function)`() {
        val outputSchema = runComputeSchemaSubsetScript(
            schemaPath = resolvedAwsClassicSchemaPath(),
            name = "aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter",
            context = "type",
        )

        val decodedOutputSchema = Json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter"),
            functions = setOf("aws:fsx/getOpenZfsSnapshot:getOpenZfsSnapshot"),
        )
    }

    private fun resolvedAwsClassicSchemaPath() =
        Paths.get(SCHEMA_PATH_AWS_SUBSET_FOR_COMPUTE).absolutePathString()

    private fun assertContainsOnly(
        schema: Schema,
        functions: Set<String> = emptySet(),
        resources: Set<String> = emptySet(),
        types: Set<String> = emptySet(),
    ) {
        assertEquals(types, schema.types.keys)
        assertEquals(resources, schema.resources.keys)
        assertEquals(functions, schema.functions.keys)
    }

    private fun runComputeSchemaSubsetScript(schemaPath: String, name: String, context: String): String {
        val outputStream = ByteArrayOutputStream()

        outputStream.use {
            ComputeSchemaSubsetScript(it).main(
                listOf(
                    "--schema-path",
                    schemaPath,
                    "--name",
                    name,
                    "--context",
                    context,
                ),
            )
        }

        return outputStream.toByteArray().decodeToString()
    }

    @Serializable
    private data class Schema(
        val types: Map<String, JsonElement>,
        val resources: Map<String, JsonElement>,
        val functions: Map<String, JsonElement>,
    )
}

private const val SCHEMA_PATH_AWS_SUBSET_FOR_COMPUTE =
    "src/test/resources/schema-aws-classic-5.16.2-subset-for-compute-schema-subset-script-test.json"
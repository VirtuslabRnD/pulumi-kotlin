package org.virtuslab.pulumikotlin.scripts

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.test.assertContains

internal class ComputeSchemaSubsetScriptTest {
    @Test
    fun `should find the type itself`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolvedAwsClassicSchemaPath(),
            nameAndContexts = listOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter" to "type"),
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf(
                "aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter",
            ),
        )
    }

    @Test
    fun `should find subset when using resource (that references some types)`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolvedAwsClassicSchemaPath(),
            nameAndContexts = listOf("aws:lambda/function:Function" to "resource"),
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

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
            fullSchemaPath = resolvedAwsClassicSchemaPath(),
            nameAndContexts = listOf("aws:fsx/getOpenZfsSnapshot:getOpenZfsSnapshot" to "function"),
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter"),
            functions = setOf("aws:fsx/getOpenZfsSnapshot:getOpenZfsSnapshot"),
        )
    }

    @Test
    fun `should work when using type (that is referenced by some function)`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolvedAwsClassicSchemaPath(),
            nameAndContexts = listOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter" to "type"),
            loadFullParents = "true",
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter"),
            functions = setOf("aws:fsx/getOpenZfsSnapshot:getOpenZfsSnapshot"),
        )
    }

    @Test
    fun `should work even when there are key conflicts`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolve(SCHEMA_PATH_AZURE_NATIVE_SUBSET_WITH_IP_ALLOCATION),
            nameAndContexts = listOf("azure-native:network:IPAllocationMethod" to "type"),
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf("azure-native:network:IPAllocationMethod"),
        )
    }

    @Test
    fun `should work even when there are recursive references (regression)`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolve(SCHEMA_PATH_AZURE_NATIVE_SUBSET_WITH_RECURSION),
            nameAndContexts = listOf("azure-native:batch:AutoScaleRunResponse" to "type"),
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf(
                "azure-native:batch:AutoScaleRunResponse",
                "azure-native:batch:AutoScaleRunErrorResponse",
            ),
        )
    }

    @Test
    fun `should load full parents when --load-full-parents=true`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolve(SCHEMA_PATH_AWS_SUBSET_FOR_COMPUTE),
            nameAndContexts = listOf("aws:lambda/FunctionTracingConfig:FunctionTracingConfig" to "type"),
            loadFullParents = "true",
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            resources = setOf("aws:lambda/function:Function"),
            types = setOf(
                "aws:lambda/FunctionDeadLetterConfig:FunctionDeadLetterConfig",
                "aws:lambda/FunctionEnvironment:FunctionEnvironment",
                "aws:lambda/FunctionEphemeralStorage:FunctionEphemeralStorage",
                "aws:lambda/FunctionFileSystemConfig:FunctionFileSystemConfig",
                "aws:lambda/FunctionImageConfig:FunctionImageConfig",
                "aws:lambda/FunctionTracingConfig:FunctionTracingConfig",
                "aws:lambda/FunctionVpcConfig:FunctionVpcConfig",
                "aws:lambda/Runtime:Runtime",
            ),
        )
    }

    @Test
    fun `should shorten descriptions when --shorten-descriptions=true`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolve(SCHEMA_PATH_AWS_SUBSET_FOR_COMPUTE),
            nameAndContexts = listOf("aws:lambda/function:Function" to "resource"),
            shortenDescriptions = "true",
        )

        assertContains(
            outputSchema,
            "S3 key of an object containing the function<<shortened>> Conflicts with `filename` and `image_uri`.",
        )
    }

    @Test
    fun `should copy provider and its children to the schema if --load-provider-with-children=true`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolve(SCHEMA_PATH_AWS_SUBSET_WITH_PROVIDER_AND_SOME_TYPES),
            nameAndContexts = listOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter" to "type"),
            shortenDescriptions = "true",
            loadProviderWithChildren = "true",
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf(
                "aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter",
                "aws:index/ProviderAssumeRole:ProviderAssumeRole",
                "aws:index/ProviderAssumeRoleWithWebIdentity:ProviderAssumeRoleWithWebIdentity",
                "aws:index/ProviderDefaultTags:ProviderDefaultTags",
                "aws:index/ProviderEndpoint:ProviderEndpoint",
                "aws:index/ProviderIgnoreTags:ProviderIgnoreTags",
                "aws:index/Region:Region",
            ),
        )

        assertNotNull(decodedOutputSchema.provider)
    }

    @Test
    fun `should not copy provider and its children to the schema if --load-provider-with-children=false`() {
        val outputSchema = runComputeSchemaSubsetScript(
            fullSchemaPath = resolve(SCHEMA_PATH_AWS_SUBSET_WITH_PROVIDER_AND_SOME_TYPES),
            nameAndContexts = listOf("aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter" to "type"),
            shortenDescriptions = "true",
            loadProviderWithChildren = "false",
        )

        val decodedOutputSchema = json.decodeFromString<Schema>(outputSchema)

        assertContainsOnly(
            decodedOutputSchema,
            types = setOf(
                "aws:fsx/getOpenZfsSnapshotFilter:getOpenZfsSnapshotFilter",
            ),
        )

        assertNull(decodedOutputSchema.provider)
    }

    private fun resolve(relativePath: String) =
        Paths.get(relativePath).absolutePathString()

    private fun resolvedAwsClassicSchemaPath() =
        resolve(SCHEMA_PATH_AWS_SUBSET_FOR_COMPUTE)

    private fun assertContainsOnly(
        schema: Schema,
        functions: Set<String> = emptySet(),
        resources: Set<String> = emptySet(),
        types: Set<String> = emptySet(),
    ) {
        assertAll(
            { assertEquals(types, schema.types.keys) },
            { assertEquals(resources, schema.resources.keys) },
            { assertEquals(functions, schema.functions.keys) },
        )
    }

    @Suppress("LongParameterList")
    private fun runComputeSchemaSubsetScript(
        fullSchemaPath: String,
        nameAndContexts: List<Pair<String, String>>,
        shortenDescriptions: String? = null,
        loadFullParents: String? = null,
        loadProviderWithChildren: String? = null,
    ): String {
        val outputStream = ByteArrayOutputStream()

        outputStream.use {
            val regularArguments = listOf(
                "--full-schema-path",
                fullSchemaPath,
            )

            val nameAndContextsToBeFlattened = nameAndContexts
                .map { (name, context) ->
                    listOf("--name-and-context", name, context)
                }

            val optionalArgumentsToBeFlattened = listOfNotNull(
                toListOrNull("--shorten-descriptions", shortenDescriptions),
                toListOrNull("--load-full-parents", loadFullParents),
                toListOrNull("--load-provider-with-children", loadProviderWithChildren),
            )

            ComputeSchemaSubsetScript(it).main(
                regularArguments + (nameAndContextsToBeFlattened + optionalArgumentsToBeFlattened).flatten(),
            )
        }

        return outputStream.toByteArray().decodeToString()
    }

    private fun toListOrNull(vararg strings: String?): List<String>? {
        if (strings.any { it == null }) {
            return null
        }
        return strings.map { requireNotNull(it) }.toList()
    }

    @Serializable
    private data class Schema(
        val provider: JsonElement? = null,
        val types: Map<String, JsonElement> = emptyMap(),
        val resources: Map<String, JsonElement> = emptyMap(),
        val functions: Map<String, JsonElement> = emptyMap(),
    )

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
}

private const val SCHEMA_PATH_AZURE_NATIVE_SUBSET_WITH_IP_ALLOCATION =
    "src/test/resources/schema-azure-native-3.44.2-subset-with-ip-allocation.json"
private const val SCHEMA_PATH_AZURE_NATIVE_SUBSET_WITH_RECURSION =
    "src/test/resources/schema-azure-native-3.44.2-subset-with-recursion.json"
private const val SCHEMA_PATH_AWS_SUBSET_FOR_COMPUTE =
    "src/test/resources/schema-aws-classic-5.16.2-subset-for-compute-schema-subset-script-test.json"
private const val SCHEMA_PATH_AWS_SUBSET_WITH_PROVIDER_AND_SOME_TYPES =
    "src/test/resources/schema-aws-classic-5.16.2-subset-with-provider-and-some-types.json"

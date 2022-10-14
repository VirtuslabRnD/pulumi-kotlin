package com.virtuslab.pulumikotlin.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ParsedSchema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.referencedTypeName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File

fun main(args: Array<String>) {
    FindInterestingSchemaSubsetsForTestsScript().main(args)
}

class FindInterestingSchemaSubsetsForTestsScript : CliktCommand() {
    private val schemaPath: String by option().required()

    override fun run() {
        val parsedSchema = Decoder.decode(File(schemaPath).inputStream())

        val types = parsedSchema.types
        val propertySpecsForResource =
            parsedSchema.resources.mapValues { (_, spec) ->
                PropertySpecs(
                    spec.inputProperties.values.toList(),
                    spec.properties.values.toList(),
                )
            }

        val candidateResources = findCandidateEntities(types, propertySpecsForResource)

        val propertySpecsForFunctions =
            parsedSchema.functions.mapValues { (_, spec) ->
                PropertySpecs(
                    spec.inputs?.properties?.values?.toList().orEmpty(),
                    spec.outputs.properties.values.toList(),
                )
            }
        val candidateFunctions = findCandidateEntities(types, propertySpecsForFunctions)

        fun query(candidate: CandidateEntity): Boolean {
            val inputs = candidate.referencedInputTypes
            val outputs = candidate.referencedOutputTypes

            val q1 = with(inputs) {
                any { it.depth >= 1 } && all { it.depth < 6 } && size >= 1 && size <= 5
            }
            val q2 = with(outputs) {
                any { it.depth >= 1 } && all { it.depth < 6 } && size >= 1 && size <= 5
            }

            return q1 && q2
        }

        // criteria used to generate subset with type Either
        fun queryForOneOf(candidate: CandidateEntity): Boolean {
            val inputs = candidate.referencedInputTypes
            val outputs = candidate.referencedOutputTypes

            val q1 = with(inputs) {
                any { it.eitherCount > 0 }
            }

            // there is no outputs in classic aws with either
            val q2 = with(outputs) {
                any { it.eitherCount > 0 }
            }

            return q1 || q2
        }

        val resource = candidateResources.filter { query(it) }.take(20)
        val function = candidateFunctions.filter { query(it) }.take(20)

        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        println(serializeResource(json, parsedSchema, resource, function))
    }
}

private data class CandidateEntity(
    val name: String,
    val referencedInputTypes: List<TypeAndDetails>,
    val referencedOutputTypes: List<TypeAndDetails>,
)

private fun serializeResource(
    json: Json,
    parsedSchema: ParsedSchema,
    candidateResources: List<CandidateEntity>,
    candidateFunctions: List<CandidateEntity>,
): String {
    fun encodeTypes(candidate: CandidateEntity): Map<String, JsonElement> {
        val inputs = candidate.referencedInputTypes.map { it.typeName to parsedSchema.types.get(it.typeName) }
        val outputs = candidate.referencedOutputTypes.map { it.typeName to parsedSchema.types.get(it.typeName) }

        return (inputs + outputs).toSet().map {
            it.first to json.encodeToJsonElement(it.second)
        }
            .toMap()
    }

    val types = candidateResources.flatMap { entity -> encodeTypes(entity).map { it.toPair() } } +
        candidateFunctions.flatMap { entity -> encodeTypes(entity).map { it.toPair() } }

    val resourceBody = candidateResources.associate { it.name to parsedSchema.resources.get(it.name) }
    val functionBody = candidateFunctions.associate { it.name to parsedSchema.functions.get(it.name) }

    val finalJsonObject = JsonObject(
        mapOf(
            "resources" to JsonObject(resourceBody.mapValues { (_, value) -> json.encodeToJsonElement(value) }),
            "functions" to JsonObject(functionBody.mapValues { (_, value) -> json.encodeToJsonElement(value) }),
            "types" to JsonObject(types.toMap()),
        ),
    )

    return json.encodeToString(finalJsonObject)
}

private data class PropertySpecs(
    val input: List<SchemaModel.Property>,
    val output: List<SchemaModel.Property>,
)

private fun findCandidateEntities(
    types: TypesMap,
    propertySpecs: Map<String, PropertySpecs>,
): List<CandidateEntity> {
    return propertySpecs.map { (name, specs) ->
        try {
            val referencedInputTypes = specs.input.flatMap { spec ->
                allReferencedTypes(types, spec)
            }
            val referencedOutputTypes = specs.output.flatMap { spec ->
                allReferencedTypes(types, spec)
            }

            CandidateEntity(name, referencedInputTypes, referencedOutputTypes)
        } catch (e: Exception) {
            CandidateEntity(name, emptyList(), emptyList())
        }
    }
}

private data class TypeAndDetails(val typeName: String, val depth: Int, val eitherCount: Int)

private fun allReferencedTypes(
    types: TypesMap,
    spec: SchemaModel.Property,
    depth: Int = 0,
    eitherCount: Int = 0,
    visited: Set<SchemaModel.Property> = emptySet(),
): List<TypeAndDetails> {
    // temporary solution to avoid StackOverflow on circular references
    if (spec in visited) {
        return emptyList()
    }

    return when (spec) {
        is SchemaModel.ArrayProperty -> allReferencedTypes(types, spec.items, depth + 1, eitherCount, visited)
        is SchemaModel.MapProperty -> allReferencedTypes(
            types,
            spec.additionalProperties,
            depth + 1,
            eitherCount,
            visited,
        )

        is SchemaModel.ObjectProperty -> spec.properties.values.flatMap {
            allReferencedTypes(
                types,
                it,
                depth + 1,
                eitherCount,
                visited,
            )
        }

        is SchemaModel.OneOfProperty -> spec.oneOf.flatMap {
            allReferencedTypes(
                types,
                it,
                depth + 1,
                eitherCount + 1,
                visited,
            )
        }

        is SchemaModel.ReferenceProperty -> {
            val typeName = spec.ref.referencedTypeName
            val theType = TypeAndDetails(typeName, depth, eitherCount)
            val foundSpec = types.get(typeName)
            if (foundSpec == null) {
                error("could not find")
            } else {
                listOf(theType) + allReferencedTypes(types, foundSpec, depth + 1, eitherCount, visited.plus(foundSpec))
            }
        }

        is SchemaModel.StringEnumProperty -> emptyList()
        is SchemaModel.StringProperty -> emptyList()
        is SchemaModel.BooleanProperty -> emptyList()
        is SchemaModel.IntegerProperty -> emptyList()
        is SchemaModel.NumberProperty -> emptyList()
    }
}

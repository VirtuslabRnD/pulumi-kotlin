package com.virtuslab.pulumikotlin.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ParsedSchema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.withoutThePrefix
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
    val input: List<Resources.PropertySpecification>,
    val output: List<Resources.PropertySpecification>,
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
    spec: Resources.PropertySpecification,
    depth: Int = 0,
    eitherCount: Int = 0,
    visited: Set<Resources.PropertySpecification> = emptySet(),
): List<TypeAndDetails> {
    // temporary solution to avoid StackOverflow on circular references
    if (spec in visited) {
        return emptyList()
    }

    return when (spec) {
        is Resources.ArrayProperty -> allReferencedTypes(types, spec.items, depth + 1, eitherCount, visited)
        is Resources.MapProperty -> allReferencedTypes(
            types,
            spec.additionalProperties,
            depth + 1,
            eitherCount,
            visited,
        )

        is Resources.ObjectProperty -> spec.properties.values.flatMap {
            allReferencedTypes(
                types,
                it,
                depth + 1,
                eitherCount,
                visited,
            )
        }

        is Resources.OneOf -> spec.oneOf.flatMap { allReferencedTypes(types, it, depth + 1, eitherCount + 1, visited) }

        is Resources.ReferredProperty -> {
            val typeName = spec.`$ref`.withoutThePrefix()
            val theType = TypeAndDetails(typeName, depth, eitherCount)
            val foundSpec = types.get(typeName)
            if (foundSpec == null) {
                error("could not find")
//                listOf(theType)
            } else {
                listOf(theType) + allReferencedTypes(types, foundSpec, depth + 1, eitherCount, visited.plus(foundSpec))
            }
        }

        is Resources.StringEnumProperty -> emptyList()
        is Resources.StringProperty -> emptyList()
        is Resources.BooleanProperty -> emptyList()
        is Resources.IntegerProperty -> emptyList()
        is Resources.NumberProperty -> emptyList()
    }
}

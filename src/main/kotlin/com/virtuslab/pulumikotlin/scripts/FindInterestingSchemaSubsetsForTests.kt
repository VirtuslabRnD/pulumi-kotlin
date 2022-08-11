package com.virtuslab.pulumikotlin.scripts

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File


data class CandidateEntity(
    val name: String,
    val referencedInputTypes: List<TypeAndDepth>,
    val referencedOutputTypes: List<TypeAndDepth>
)

fun main() {
    val schema = "/Users/mfudala/workspace/pulumi-kotlin/src/main/resources/schema-aws-classic.json"
    val parsedSchema = Decoder.decode(File(schema).inputStream())

    val types = parsedSchema.types
    val propertySpecsForResource =
        parsedSchema.resources.mapValues { (_, spec) ->
            PropertySpecs(
                spec.inputProperties.values.toList(),
                spec.properties.values.toList()
            )
        }

    val candidateResources = findCandidateEntities(types, propertySpecsForResource)

    val propertySpecsForFunctions =
        parsedSchema.functions.mapValues { (_, spec) ->
            PropertySpecs(
                spec.inputs?.properties?.values?.toList().orEmpty(),
                spec.outputs.properties.values.toList()
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

        val q3 = inputs.any {
            outputs.map { it.typeName }.contains(it.typeName)
        } && inputs.any { !outputs.map { it.typeName }.contains(it.typeName) }

        return q1 && q2
    }

    fun query2(candidate: CandidateEntity): Boolean {
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

    val resource = candidateResources.first { query(it) }
    val function = candidateFunctions.first { query2(it) }

    val json = Json {
        prettyPrint = true
    }

    println(serializeResource(json, parsedSchema, resource, function))
}

fun serializeResource(
    json: Json,
    parsedSchema: ParsedSchema,
    candidateResource: CandidateEntity,
    candidateFunction: CandidateEntity
): String {

    fun encodeTypes(candidate: CandidateEntity): Map<String, JsonElement> {
        val inputs = candidate.referencedInputTypes.map { it.typeName to parsedSchema.types.get(it.typeName) }
        val outputs = candidate.referencedOutputTypes.map { it.typeName to parsedSchema.types.get(it.typeName) }

        return (inputs + outputs).toSet().map {
            it.first to json.encodeToJsonElement(it.second)
        }
            .toMap()
    }

    val resourceBody = parsedSchema.resources.get(candidateResource.name)
    val functionBody = parsedSchema.functions.get(candidateFunction.name)

    val types = encodeTypes(candidateResource) + encodeTypes(candidateFunction)

    val finalJsonObject = JsonObject(
        mapOf(
            "resources" to JsonObject(mapOf(candidateResource.name to json.encodeToJsonElement(resourceBody))),
            "functions" to JsonObject(mapOf(candidateFunction.name to json.encodeToJsonElement(functionBody))),
            "types" to JsonObject(types)
        )
    )

    return json.encodeToString(finalJsonObject)
}

data class PropertySpecs(
    val input: List<Resources.PropertySpecification>,
    val output: List<Resources.PropertySpecification>
)

fun findCandidateEntities(
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
        } catch(e: Exception) {
            CandidateEntity(name, emptyList(), emptyList())
        }
    }
}

data class TypeAndDepth(val typeName: String, val depth: Int)

fun allReferencedTypes(types: TypesMap, spec: Resources.PropertySpecification, depth: Int = 0): List<TypeAndDepth> {
    return when (spec) {
        is Resources.ArrayProperty -> allReferencedTypes(types, spec.items, depth + 1)
        is Resources.MapProperty -> allReferencedTypes(types, spec.additionalProperties, depth + 1)
        is Resources.ObjectProperty -> spec.properties.values.flatMap { allReferencedTypes(types, it, depth + 1) }
        is Resources.OneOf -> spec.oneOf.flatMap { allReferencedTypes(types, it, depth + 1) }

        is Resources.ReferredProperty -> {
            val typeName = spec.`$ref`.withoutThePrefix()
            val theType = TypeAndDepth(typeName, depth)
            val foundSpec = types.get(typeName)
            if (foundSpec == null) {
                error("could not find")
//                listOf(theType)
            } else {
                listOf(theType) + allReferencedTypes(types, foundSpec, depth + 1)
            }
        }

        is Resources.StringEnumProperty -> emptyList()
        is Resources.StringProperty -> emptyList()
        is Resources.BooleanProperty -> emptyList()
        is Resources.IntegerProperty -> emptyList()
        is Resources.NumberProperty -> emptyList()

    }
}
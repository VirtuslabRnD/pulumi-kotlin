package com.virtuslab.pulumikotlin.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Decoder
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ArrayProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.MapProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ObjectProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.OneOfProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PrimitiveProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Property
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferenceProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferencingOtherTypesProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringEnumProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.referencedTypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.transformKeys
import com.virtuslab.pulumikotlin.codegen.utils.filterNotNullValues
import com.virtuslab.pulumikotlin.scripts.Context.Function
import com.virtuslab.pulumikotlin.scripts.Context.Resource
import com.virtuslab.pulumikotlin.scripts.Context.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

fun main(args: Array<String>) {
    ComputeSchemaSubsetScript().main(args)
}

/**
 * Example program arguments
 * --schema-path src/main/resources/schema-aws-classic.json
 * --name gcp:compute/instance:Instance
 * --context resource
 */
class ComputeSchemaSubsetScript(outputStream: OutputStream = System.out) : CliktCommand() {

    private val printStream = PrintStream(outputStream)

    private val schemaPath: String by option().required()
    private val name: String by option().required()
    private val context: Context by option().enum<Context>().required()

    override fun run() {
        val parsedSchema = Decoder.decode(File(schemaPath).inputStream())

        val propertiesWithNameAndContext = with(parsedSchema) {
            listOf(
                extractProperties(types, Type) {
                    listOf(it)
                },
                extractProperties(resources, Resource) {
                    it.inputProperties.values + it.properties.values
                },
                extractProperties(functions, Function) {
                    it.inputs?.properties?.values.orEmpty() + it.outputs.properties.values
                },
            )
        }

        val propertiesByNameAndContext = propertiesWithNameAndContext
            .flatten()
            .toMap()
            .lowercaseKeys()

        val childrenByNameAndContext = computeTransitiveClosures(propertiesByNameAndContext).lowercaseKeys()
        val parentsByNameAndContext = inverse(childrenByNameAndContext).lowercaseKeys()

        val chosenKey = NameWithContext(name, context)
        val requiredChildren = childrenByNameAndContext[chosenKey] ?: error("could not find $name children ($context)")
        val requiredParents = parentsByNameAndContext[chosenKey] ?: run {
            println("could not find $name parents ($context)")
            References.empty()
        }

        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        val noDataLossSchemaModel = json.decodeFromString<NoDataLossSchemaModel>(File(schemaPath).readText())

        fun <V> getFiltered(map: Map<String, V>, context: Context) =
            map.filterKeys {
                val key = NameWithContext(it, context)
                key == chosenKey || key in requiredChildren.references || key in requiredParents.references
            }

        val newSchema = with(noDataLossSchemaModel) {
            NoDataLossSchemaModel(
                types = getFiltered(types, Type),
                resources = getFiltered(resources, Resource),
                functions = getFiltered(functions, Function),
            )
        }

        printStream.println(json.encodeToString(newSchema))
    }

    private fun <V> extractProperties(map: Map<String, V>, context: Context, propertyExtractor: (V) -> List<Property>) =
        map
            .mapValues { propertyExtractor(it.value) }
            .mapKeys { NameWithContext(it.key, context) }
            .filterNotNullValues()
            .map { it.toPair() }

    private fun computeTransitiveClosures(allProperties: Map<NameWithContext, List<Property>>): TypeKeysToChildren {
        return allProperties
            .mapValues { (key, value) ->
                value
                    .map { computeTransitiveClosure(allProperties, PropertyWithContext(it, key.context)) }
                    .fold(References.empty(), References::merge)
            }
    }

    private fun computeTransitiveClosure(
        allProperties: Map<NameWithContext, List<Property>>,
        propertyWithContext: PropertyWithContext,
        visited: Set<NameWithContext> = emptySet(),
    ): References {
        val children = when (val property = propertyWithContext.property) {
            is ReferencingOtherTypesProperty -> {
                References.from(
                    getInnerProperties(property).flatMap {
                        computeTransitiveClosure(allProperties, PropertyWithContext(it, Type), visited).references
                    },
                )
            }

            is ReferenceProperty -> {
                getReference(allProperties, property, visited)
            }

            is PrimitiveProperty, is StringEnumProperty -> References.empty()
        }

        return children
    }

    private fun getInnerProperties(property: ReferencingOtherTypesProperty): List<Property> {
        return when (property) {
            is ArrayProperty -> listOf(property.items)
            is MapProperty -> listOf(property.additionalProperties)
            is OneOfProperty -> property.oneOf
            is ObjectProperty -> property.properties.values.toList()
        }
    }

    private fun getReference(
        map: Map<NameWithContext, List<Property>>,
        property: ReferenceProperty,
        visited: Set<NameWithContext>,
    ): References {
        val typeName = property.ref.referencedTypeName
        if (typeName.startsWith("pulumi")) {
            return References.empty()
        }
        val key = NameWithContext(typeName, Type)
        if (visited.contains(key)) {
            return References.from(key)
        }
        val foundProperties = map[key]
        if (foundProperties == null) {
            println("could not find $typeName")
            return References.from(key)
        }

        val recursive = foundProperties
            .map {
                val foundPropertyWithContext = PropertyWithContext(it, Type)
                computeTransitiveClosure(map, foundPropertyWithContext, visited + key)
            }
            .fold(References.empty(), References::merge)

        return recursive.add(key)
    }

    private fun inverse(typeKeysToChildren: TypeKeysToChildren): TypeKeysToParents {
        val inverseMap = mutableMapOf<NameWithContext, References>()
        typeKeysToChildren.forEach { (key, values) ->
            values.references.forEach { value ->
                inverseMap.merge(value, References.from(key), References::merge)
            }
        }
        return inverseMap
    }
}

@Serializable
private data class NoDataLossSchemaModel(
    val types: Map<String, JsonElement>,
    val resources: Map<String, JsonElement>,
    val functions: Map<String, JsonElement>,
)

private typealias Name = String
private typealias TypeKeysToChildren = Map<NameWithContext, References>
private typealias TypeKeysToParents = Map<NameWithContext, References>

private enum class Context {
    Function, Type, Resource;
}

private data class PropertyWithContext(val property: Property, val context: Context)

private data class NameWithContext(val name: Name, val context: Context) {
    fun withLowercaseTypeName() = copy(name = name.lowercase())
}

private fun <V : Any> Map<NameWithContext, V>.lowercaseKeys() =
    this.transformKeys { it.withLowercaseTypeName() }

private class References private constructor(val references: Set<NameWithContext>) {
    fun add(one: NameWithContext) =
        from(references + one)

    fun merge(other: References) =
        from(references + other.references)

    companion object {
        fun empty() =
            References(emptySet())

        fun from(vararg namesWithContext: NameWithContext) =
            from(namesWithContext.asIterable())

        fun from(references: Iterable<NameWithContext>) =
            References(references.toSet().transformKeys { it.withLowercaseTypeName() })
    }
}

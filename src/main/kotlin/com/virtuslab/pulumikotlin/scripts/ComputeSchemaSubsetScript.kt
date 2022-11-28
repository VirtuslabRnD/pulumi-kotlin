package com.virtuslab.pulumikotlin.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
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
import com.virtuslab.pulumikotlin.codegen.step2intermediate.transformKeys
import com.virtuslab.pulumikotlin.codegen.utils.filterNotNullValues
import com.virtuslab.pulumikotlin.codegen.utils.letIf
import com.virtuslab.pulumikotlin.codegen.utils.shorten
import com.virtuslab.pulumikotlin.scripts.Context.Function
import com.virtuslab.pulumikotlin.scripts.Context.Provider
import com.virtuslab.pulumikotlin.scripts.Context.Resource
import com.virtuslab.pulumikotlin.scripts.Context.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import mu.KotlinLogging
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

fun main(args: Array<String>) {
    ComputeSchemaSubsetScript().main(args)
}

private const val HELP =
    """
        Example invocation:
        ```
        programName \ 
        --schema-path src/main/resources/schema-aws-classic.json \
        --name gcp:compute/instance:Instance \
        --context resource
        ```
    """

class ComputeSchemaSubsetScript(outputStream: OutputStream = System.out) : CliktCommand(help = HELP) {

    private val logger = KotlinLogging.logger {}
    private val printStream = PrintStream(outputStream)

    private val schemaPath: String by option().required()
    private val context: Context by option().enum<ExposedContext>().convert { it.toContext() }.required()
    private val name: String by option().required()
    private val shortenDescriptions: Boolean by option().boolean(default = false)
    private val loadProviderWithChildren: Boolean by option().boolean(default = true)

    private val loadFullParentsHelp =
        """
            Whether to include parents and load parents' references. 
            For example, given this input schema:
            
                (simplified model, 'B -> {C}' means that 'B has reference to C')
                
                ```
                types:
                    B -> {C}
                    C -> {D}
                    D -> {}
                    E -> {}
                resources:
                    A -> {B}
                ```
                
            and options: name=C, context=type, the following schema will be computed:
            
            - with loadFullParents=true:
                ```
                types:
                    B -> {C}
                    C -> {D}
                    D -> {}
                resources:
                    A -> {B}
                ```
                
            - with loadFullParents=false:
                ```
                types:
                    C -> {D}
                    D -> {}
                resources:
                    <<empty>>
                ```
        """
    private val loadFullParents: Boolean by option(help = loadFullParentsHelp).boolean(default = false)

    override fun run() {
        val parsedSchema = Decoder.decode(File(schemaPath).inputStream())

        val providerNameWithContext = NameWithContext("pulumi:providers:${parsedSchema.provider}", Provider)

        val allPropertiesToBeFlattened = with(parsedSchema) {
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
                extractProperties(providerNameWithContext.name, provider, providerNameWithContext.context) {
                    it.inputProperties.values + it.properties.values
                },
            )
        }

        val propertiesByNameWithContext = allPropertiesToBeFlattened
            .flatten()
            .toMap()
            .lowercaseKeys()

        val chosenKey = NameWithContext(name, context)

        val childrenByNameWithContext = computeTransitiveClosures(propertiesByNameWithContext).lowercaseKeys()
        val parentsByNameWithContext = inverse(childrenByNameWithContext).lowercaseKeys()

        val childrenOfChosenNameWithContext = childrenByNameWithContext[chosenKey]
            ?: error("Could not find $name children ($context)")
        val parentsOfChosenNameWithContextAndTheirChildren = parentsByNameWithContext[chosenKey] ?: run {
            logger.info("Could not find $name parents ($context)")
            References.empty()
        }
        val childrenOfProvider = childrenByNameWithContext[providerNameWithContext] ?: References.empty()

        val allTypesThatWereSomehowReferenced = childrenOfChosenNameWithContext
            .letIf(loadProviderWithChildren) {
                it.merge(childrenOfProvider)
            }
            .letIf(loadFullParents) {
                it.merge(parentsOfChosenNameWithContextAndTheirChildren)
            }

        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        val noDataLossSchemaModel = json.decodeFromString<NoDataLossSchemaModel>(File(schemaPath).readText())

        fun <V> getFiltered(map: Map<String, V>, context: Context) =
            map.filterKeys {
                val key = NameWithContext(it, context)
                key == chosenKey || allTypesThatWereSomehowReferenced.contains(key)
            }

        val newSchema = with(noDataLossSchemaModel) {
            copy(
                provider = if (loadProviderWithChildren) provider else null,
                types = getFiltered(types, Type),
                resources = getFiltered(resources, Resource),
                functions = getFiltered(functions, Function),
            )
        }

        val encodedNewSchema = json.encodeToJsonElement(removeDescriptionFieldIfNeeded(newSchema))

        printStream.println(encodedNewSchema)
    }

    private fun <V> extractProperties(map: Map<String, V>, context: Context, propertyExtractor: (V) -> List<Property>) =
        map
            .mapValues { propertyExtractor(it.value) }
            .mapKeys { NameWithContext(it.key, context) }
            .filterNotNullValues()
            .map { it.toPair() }

    private fun <V> extractProperties(
        name: String,
        value: V?,
        context: Context,
        propertyExtractor: (V) -> List<Property>,
    ) =
        extractProperties(mapOf(name to value).filterNotNullValues(), context, propertyExtractor)

    private fun computeTransitiveClosures(
        allProperties: Map<NameWithContext, List<Property>>,
    ): ChildrenByNameWithContext {
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
        return when (val property = propertyWithContext.property) {
            is ReferencingOtherTypesProperty -> {
                References.from(
                    getInnerProperties(property).flatMap {
                        computeTransitiveClosure(allProperties, PropertyWithContext(it, Type), visited).value
                    },
                )
            }

            is ReferenceProperty -> {
                getReferences(allProperties, property, visited)
            }

            is PrimitiveProperty, is StringEnumProperty -> References.empty()
        }
    }

    private fun getInnerProperties(property: ReferencingOtherTypesProperty): List<Property> {
        return when (property) {
            is ArrayProperty -> listOf(property.items)
            is MapProperty -> listOf(property.additionalProperties)
            is OneOfProperty -> property.oneOf
            is ObjectProperty -> property.properties.values.toList()
        }
    }

    private fun getReferences(
        allProperties: Map<NameWithContext, List<Property>>,
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

        val foundProperties = allProperties[key]
        if (foundProperties == null) {
            logger.info("Could not find $typeName")
            return References.from(key)
        }

        val recursiveReferences = foundProperties
            .map {
                val foundPropertyWithContext = PropertyWithContext(it, Type)
                computeTransitiveClosure(allProperties, foundPropertyWithContext, visited + key)
            }
            .fold(References.empty(), References::merge)

        return recursiveReferences.add(key)
    }

    private fun inverse(
        childrenByNameWithContext: ChildrenByNameWithContext,
    ): ParentsByNameWithContext {
        val inverseMap = mutableMapOf<NameWithContext, References>()
        childrenByNameWithContext
            .entries
            .forEach { (key, references) ->
                val allReferences = References.from(key).merge(references)
                references.value.forEach { value ->
                    inverseMap.merge(value, allReferences, References::merge)
                }
            }
        return inverseMap
    }

    private fun removeDescriptionFieldIfNeeded(schemaModel: NoDataLossSchemaModel): NoDataLossSchemaModel {
        if (!shortenDescriptions) {
            return schemaModel
        }

        return schemaModel.copy(
            types = schemaModel.types.mapValues { removeDescriptionField(it.value) },
            functions = schemaModel.functions.mapValues { removeDescriptionField(it.value) },
            resources = schemaModel.resources.mapValues { removeDescriptionField(it.value) },
        )
    }

    private fun removeDescriptionField(jsonElement: JsonElement): JsonElement {
        return when (jsonElement) {
            is JsonObject -> {
                val mapped = jsonElement.mapValues { (key, value) ->
                    if (key == "description" && value is JsonPrimitive) {
                        JsonPrimitive(value.contentOrNull?.shorten(desiredLength = 100))
                    } else {
                        removeDescriptionField(value)
                    }
                }
                JsonObject(mapped)
            }

            is JsonArray -> {
                JsonArray(jsonElement.map { removeDescriptionField(it) })
            }

            is JsonPrimitive -> {
                jsonElement
            }
        }
    }
}

@Serializable
private data class NoDataLossSchemaModel(
    val name: JsonElement? = null,
    val displayName: JsonElement? = null,
    val version: JsonElement? = null,
    val meta: JsonElement? = null,
    val config: JsonElement? = null,
    val language: JsonElement? = null,
    val provider: JsonElement? = null,
    val types: Map<String, JsonElement>,
    val resources: Map<String, JsonElement>,
    val functions: Map<String, JsonElement>,
)

private typealias Name = String
private typealias ChildrenByNameWithContext = Map<NameWithContext, References>
private typealias ParentsByNameWithContext = Map<NameWithContext, References>

private enum class ExposedContext {
    Function,
    Type,
    Resource,
    ;

    fun toContext() = when (this) {
        Function -> Context.Function
        Type -> Context.Type
        Resource -> Context.Resource
    }
}

private enum class Context {
    Function,
    Type,
    Resource,
    Provider,
}

private data class PropertyWithContext(val property: Property, val context: Context)

private data class NameWithContext(val name: Name, val context: Context) {
    fun withLowercaseTypeName() = copy(name = name.lowercase())
}

private fun <V : Any> Map<NameWithContext, V>.lowercaseKeys() =
    this.transformKeys { it.withLowercaseTypeName() }

private class References private constructor(private val underlyingValue: Set<NameWithContext>) {
    val value by lazy { underlyingValue.transformKeys { it.withLowercaseTypeName() } }

    fun contains(name: NameWithContext) = value.contains(name)

    fun add(one: NameWithContext) = from(underlyingValue + one)

    fun merge(other: References) = from(underlyingValue + other.underlyingValue)

    companion object {
        fun empty() = from(emptySet())

        fun from(vararg namesWithContext: NameWithContext) = from(namesWithContext.toSet())

        fun from(references: Set<NameWithContext>) = References(references)

        fun from(references: Iterable<NameWithContext>) = from(references.toSet())
    }
}

private fun RawOption.boolean(default: Boolean) =
    choice("true", "false").convert { it.toBooleanStrict() }.default(default, default.toString())

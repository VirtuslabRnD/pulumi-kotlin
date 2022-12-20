package com.virtuslab.pulumikotlin.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.transformValues
import com.github.ajalt.clikt.parameters.types.choice
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
import com.virtuslab.pulumikotlin.codegen.utils.capitalize
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
        Examples:
        
        Create gcp schema subset with gcp:compute/instance:Instance resource:
        
        ```
        programName \ 
        --full-schema-path src/main/resources/schema-gcp-classic.json \
        --name-and-context gcp:compute/instance:Instance resource
        ```
        
        Update existing gcp schema subset by adding gcp:compute/instance:Instance resource and 
        gcp:compute/getImage:getImage function: 
        
        ```
        programName \ 
        --full-schema-path src/main/resources/schema-gcp-classic.json \
        --existing-schema-subset-path src/test/resources/schema-gcp-classic-6.39.0-subset-lb-ip-ranges.json \
        --name-and-context gcp:compute/instance:Instance resource \
        --name-and-context gcp:compute/getImage:getImage function
        ```
    """

class ComputeSchemaSubsetScript(outputStream: OutputStream = System.out) : CliktCommand(help = HELP) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val logger = KotlinLogging.logger {}
    private val printStream = PrintStream(outputStream)

    private val fullSchemaPath: String by option().required().help(
        "Path to the full schema (can be downloaded from provider's GitHub repository, for example: " +
            "https://github.com/pulumi/pulumi-gcp/blob/master/provider/cmd/pulumi-resource-gcp/schema.json)",
    )
    private val existingSchemaSubsetPath: String? by option().help(
        "Optional path to an existing schema subset. " +
            "Functions, types and resources present there will be present in the resulting schema.",
    )
    private val nameAndContexts: List<NameWithContext>
        by option("--name-and-context")
            .transformValues(2) { NameWithContext(it[0], ExposedContext.valueOf(it[1].capitalize()).toContext()) }
            .multiple()
            .help(
                "Specify multiple types/functions/resources to include in the schema subset" +
                    "For example, to include Instance resource and getImage function, you can pass these: " +
                    "--name-and-context gcp:compute/instance:Instance resource " +
                    "--name-and-context gcp:compute/getImage:getImage function",
            )

    private val shortenDescriptions: Boolean by option().boolean(default = false).help(
        "Reduce descriptions length (100 characters)",
    )
    private val loadProviderWithChildren: Boolean by option().boolean(default = true).help(
        "Include 'provider' resource in the final schema (and any types it depends on)",
    )

    private val loadFullParentsHelp =
        """
            Include parents of the given types/resources/functions (--name-and-contexts) 
            and load any types/resources/functions these parents reference. 
            
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
        val parsedSchema = Decoder.decode(File(fullSchemaPath).inputStream())
        val parsedExistingSchema = existingSchemaSubsetPath?.let { Decoder.decode(File(it).inputStream()) }

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

        val propertiesByNameWithContextFromExistingSchema = parsedExistingSchema
            ?.let { schema ->
                with(schema) {
                    types.keys.map { NameWithContext(it, Type) } +
                        resources.keys.map { NameWithContext(it, Resource) } +
                        functions.keys.map { NameWithContext(it, Function) }
                }
            }
            .orEmpty()

        val allNamesWithContext = nameAndContexts + propertiesByNameWithContextFromExistingSchema

        val childrenByNameWithContext = computeTransitiveClosures(propertiesByNameWithContext).lowercaseKeys()
        val parentsByNameWithContext = inverse(childrenByNameWithContext).lowercaseKeys()

        val allTypesThatWereSomehowReferenced = allNamesWithContext.fold(References.empty()) { acc, chosenKey ->
            val (name, context) = chosenKey

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

            acc.merge(allTypesThatWereSomehowReferenced)
        }

        val noDataLossSchemaModel = json.decodeFromString<NoDataLossSchemaModel>(File(fullSchemaPath).readText())

        fun <V> getFiltered(map: Map<String, V>, context: Context) =
            map.filterKeys {
                val key = NameWithContext(it, context)
                allNamesWithContext.contains(key) || allTypesThatWereSomehowReferenced.contains(key)
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

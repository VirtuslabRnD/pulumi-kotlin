package org.virtuslab.pulumikotlin.codegen.step1schemaparse

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.ArrayType
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.BooleanType
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.IntegerType
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.NumberType
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.ObjectType
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.StringType

typealias TypesMap = Map<String, SchemaModel.RootTypeProperty>
typealias FunctionsMap = Map<String, SchemaModel.Function>
typealias ResourcesMap = Map<String, SchemaModel.Resource>

@Suppress(
    "SERIALIZER_TYPE_INCOMPATIBLE", // https://github.com/VirtuslabRnD/pulumi-kotlin/issues/63
    "LongParameterList",
)
object SchemaModel {
    object PropertySerializer : JsonContentPolymorphicSerializer<Property>(Property::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out Property> =
            with(element) {
                when {
                    isReference() -> ReferenceProperty.serializer()
                    isOneOf() -> OneOfProperty.serializer()
                    isMap() -> MapProperty.serializer()
                    isObject() -> ObjectProperty.serializer()
                    isEnum() -> StringEnumProperty.serializer()
                    hasType("array") -> ArrayProperty.serializer()
                    hasType("string") -> StringProperty.serializer()
                    hasType("boolean") -> BooleanProperty.serializer()
                    hasType("integer") -> IntegerProperty.serializer()
                    hasType("number") -> NumberProperty.serializer()
                    else -> {
                        error("Unknown $element")
                    }
                }
            }

        private fun JsonElement.isReference() = has("\$ref")

        private fun JsonElement.isOneOf() = has("oneOf")

        private fun JsonElement.isMap() = has("additionalProperties") && !has("properties")

        private fun JsonElement.isObject() = has("properties") || hasType("object")

        private fun JsonElement.isEnum() = has("enum")

        private fun JsonElement.hasType(type: String) = has("type", type)

        private fun JsonElement.has(key: String) = (this as? JsonObject)?.contains(key) ?: false

        private fun JsonElement.has(key: String, value: String) =
            (this as? JsonObject)?.get(key)?.jsonPrimitive?.content == value
    }

    data class Schema(
        val providerName: String,
        val providerDisplayName: String?,
        val description: String?,
        val config: JsonElement?,
        val provider: Resource?,
        val types: TypesMap = emptyMap(),
        val functions: FunctionsMap = emptyMap(),
        val resources: ResourcesMap = emptyMap(),
        val metadata: Metadata?,
        val providerLanguage: PackageLanguage?,
    )

    @Serializable
    data class RawFullProviderSchema(
        val name: String,
        val displayName: String? = null,
        val version: String? = null,
        val description: String? = null,
        val keywords: List<String>? = null,
        val homepage: String? = null,
        val license: String? = null,
        val attribution: String? = null,
        val repository: String? = null,
        val publisher: String? = null,
        val logoUrl: String? = null,
        val pluginDownloadURL: String? = null,
        val config: JsonElement? = null,
        val provider: Resource? = null,
        val types: TypesMap = emptyMap(),
        val functions: FunctionsMap = emptyMap(),
        val resources: ResourcesMap = emptyMap(),
        val meta: Metadata? = null,
        val language: PackageLanguage? = null,
    )

    @Serializable
    @JvmInline
    value class Language(val map: Map<String, JsonElement>?)

    @Serializable
    enum class PropertyType {
        @SerialName("array")
        ArrayType,

        @SerialName("string")
        StringType,

        @SerialName("object")
        ObjectType,

        @SerialName("boolean")
        BooleanType,

        @SerialName("integer")
        IntegerType,

        @SerialName("number")
        NumberType,
    }

    @Serializable
    @JvmInline
    value class PropertyName(
        val value: String,
    )

    @Serializable
    data class StringSingleEnum(
        val name: String? = null,
        val value: JsonElement? = null,
        val description: String? = null,
        val deprecationMessage: String? = null,
        val default: JsonElement? = null,
    )

    @Serializable
    data class StringEnumProperty(
        val enum: List<StringSingleEnum>,
        override val type: PropertyType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val isOverlay: Boolean = false,
        override val plain: Boolean = false,
    ) : RootTypeProperty

    @Serializable
    data class StringProperty(
        override val type: PropertyType = StringType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : PrimitiveProperty

    @Serializable
    data class BooleanProperty(
        override val type: PropertyType = BooleanType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : PrimitiveProperty

    @Serializable
    data class IntegerProperty(
        override val type: PropertyType = IntegerType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : PrimitiveProperty

    @Serializable
    data class NumberProperty(
        override val type: PropertyType = NumberType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : PrimitiveProperty

    @Serializable
    class ArrayProperty(
        val items: Property,
        override val type: PropertyType = ArrayType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : GenericTypeProperty

    @Serializable
    data class ReferenceProperty(
        @SerialName("\$ref")
        val ref: SpecificationReference,
        override val type: PropertyType? = null,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : Property {
        val referencedTypeName: String
            get() = ref.referencedTypeName

        fun isAssetOrArchive() = referencedTypeName == "pulumi.json#/Asset"

        fun isArchive() = referencedTypeName == "pulumi.json#/Archive"

        fun isAny() = referencedTypeName == "pulumi.json#/Any"

        fun isJson() = referencedTypeName == "pulumi.json#/Json"
    }

    @Serializable
    @JvmInline
    value class SpecificationReference(val value: String) {
        val referencedTypeName: String
            get() = value.removePrefix("#/types/")
    }

    @Serializable
    data class Discriminator(
        val propertyName: String,
        val mapping: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class OneOfProperty(
        val oneOf: List<Property>,
        val discriminator: Discriminator? = null,
        override val type: PropertyType? = null,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : GenericTypeProperty

    @Serializable
    data class ObjectProperty(
        val properties: Map<PropertyName, Property> = emptyMap(),
        val additionalProperties: Property? = null,
        val required: Set<PropertyName> = emptySet(),
        override val type: PropertyType = ObjectType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val isOverlay: Boolean = false,
        override val plain: Boolean = false,
    ) : RootTypeProperty,
        ReferencingOtherTypesProperty

    @Serializable
    data class MapProperty(
        val additionalProperties: Property,
        override val type: PropertyType = ObjectType,
        override val default: JsonElement? = null,
        override val defaultInfo: JsonElement? = null,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        override val language: Language? = null,
        override val willReplaceOnChanges: Boolean = false,
        override val replaceOnChanges: Boolean = false,
        override val secret: Boolean = false,
        override val const: JsonElement? = null,
        override val plain: Boolean = false,
    ) : GenericTypeProperty

    @Serializable(with = PropertySerializer::class)
    sealed interface Property {
        val type: PropertyType?
        val default: JsonElement?
        val defaultInfo: JsonElement?
        val deprecationMessage: String?
        val description: String?
        val language: Language?
        val secret: Boolean
        val const: JsonElement?
        val willReplaceOnChanges: Boolean
        val replaceOnChanges: Boolean
        val plain: Boolean
    }

    @Serializable(with = PropertySerializer::class)
    sealed interface PrimitiveProperty : Property

    @Serializable(with = PropertySerializer::class)
    sealed interface ReferencingOtherTypesProperty : Property

    @Serializable(with = PropertySerializer::class)
    sealed interface GenericTypeProperty : ReferencingOtherTypesProperty

    @Serializable(with = PropertySerializer::class)
    sealed interface RootTypeProperty : Property {
        val isOverlay: Boolean
    }

    @Serializable
    data class Resource(
        val description: String? = null,
        val properties: Map<PropertyName, Property> = emptyMap(),
        val type: PropertyType? = null,
        val required: List<PropertyName> = emptyList(),
        val inputProperties: Map<PropertyName, Property> = emptyMap(),
        val requiredInputs: List<PropertyName> = emptyList(),
        val stateInputs: JsonObject? = null,
        val aliases: JsonArray? = null,
        val isComponent: Boolean = false,
        val methods: Map<String, String> = emptyMap(),
        val deprecationMessage: String? = null,
        val isOverlay: Boolean = false,
        val overlaySupportedLanguages: List<String> = emptyList(),
        val language: JsonObject? = null,
    )

    @Serializable
    data class Function(
        val inputs: ObjectProperty? = null,
        val outputs: ObjectProperty,
        val deprecationMessage: String? = null,
        val description: String? = null,
    )

    @Serializable
    data class Metadata(val moduleFormat: String? = null)

    @Serializable
    data class PackageLanguage(
        val nodejs: JsonElement? = null,
        val python: JsonElement? = null,
        val go: JsonElement? = null,
        val csharp: JsonElement? = null,
        val java: JavaPackageLanguage? = null,
    )

    @Serializable
    data class JavaPackageLanguage(
        val packages: Map<String, String> = emptyMap(),
        val basePackage: String? = null,
        val buildFiles: String? = null,
        val dependencies: Map<String, String> = emptyMap(),
        val liftSingleValueMethodReturns: Boolean = false,
    )
}

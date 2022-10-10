package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.ArrayType
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.BooleanType
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.IntegerType
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.NumberType
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.ObjectType
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PropertyType.StringType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

typealias TypesMap = Map<String, SchemaModel.RootTypeProperty>
typealias FunctionsMap = Map<String, SchemaModel.Function>
typealias ResourcesMap = Map<String, SchemaModel.Resource>

data class ParsedSchema(
    val types: TypesMap,
    val functions: FunctionsMap,
    val resources: ResourcesMap,
)

@Suppress("SERIALIZER_TYPE_INCOMPATIBLE") // https://github.com/VirtuslabRnD/pulumi-kotlin/issues/63
object SchemaModel {

    object PropertySerializer : JsonContentPolymorphicSerializer<Property>(Property::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out Property> {
            fun hasTypeEqualTo(type: String) =
                element is JsonObject &&
                    "type" in element.jsonObject &&
                    element.jsonObject.getValue("type").jsonPrimitive.content == type

            fun isMapType() =
                element is JsonObject &&
                    "additionalProperties" in element.jsonObject &&
                    "properties" !in element.jsonObject

            fun mightBeOfTypeObject() = element is JsonObject && "properties" in element.jsonObject

            return when {
                element is JsonObject && "\$ref" in element.jsonObject -> {
                    val refType = element.get("\$ref")?.jsonPrimitive?.content
                    if (refType.equals("pulumi.json#/Asset") || refType.equals("pulumi.json#/Archive")) {
                        AssetOrArchiveProperty.serializer()
                    } else {
                        ReferenceProperty.serializer()
                    }
                }
                element is JsonObject && "oneOf" in element.jsonObject -> OneOfProperty.serializer()
                isMapType() -> MapProperty.serializer()
                mightBeOfTypeObject() -> ObjectProperty.serializer()
                hasTypeEqualTo("array") -> ArrayProperty.serializer()
                hasTypeEqualTo("string") && "enum" in element.jsonObject -> StringEnumProperty.serializer()
                hasTypeEqualTo("string") -> StringProperty.serializer()
                hasTypeEqualTo("object") -> ObjectProperty.serializer()
                hasTypeEqualTo("boolean") -> BooleanProperty.serializer()
                hasTypeEqualTo("integer") -> IntegerProperty.serializer()
                hasTypeEqualTo("number") -> NumberProperty.serializer()
                else -> {
                    error("Unknown $element")
                }
            }
        }
    }

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
        val value: String,
        val description: String? = null,
        val deprecationMessage: String? = null,
        val default: JsonElement? = null,
    )

    @Serializable
    data class StringEnumProperty(
        val type: PropertyType,
        val enum: List<StringSingleEnum>,
        override val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        override val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : RootTypeProperty

    @Serializable
    data class StringProperty(
        val type: PropertyType = StringType,
        override val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        override val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PrimitiveProperty

    @Serializable
    data class BooleanProperty(
        val type: PropertyType = BooleanType,
        override val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        override val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PrimitiveProperty

    @Serializable
    data class IntegerProperty(
        val type: PropertyType = IntegerType,
        override val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        override val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PrimitiveProperty

    @Serializable
    data class NumberProperty(
        val type: PropertyType = NumberType,
        val willReplaceOnChanges: Boolean = false,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PrimitiveProperty

    @Serializable
    class ArrayProperty(
        val type: PropertyType = ArrayType,
        val items: Property,
        val willReplaceOnChanges: Boolean = false,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : GenericTypeProperty

    @Serializable
    data class ReferenceProperty(
        val type: String? = null,
        @SerialName("\$ref")
        val ref: SpecificationReference,
        val willReplaceOnChanges: Boolean = false,
        override val deprecationMessage: String? = null,
        override val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : Property

    @Serializable
    @JvmInline
    value class SpecificationReference(val value: String)

    @Serializable
    data class OneOfProperty(
        val type: String? = null,
        override val description: String? = null,
        val oneOf: List<Property>,
        override val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
        val willReplaceOnChanges: Boolean = false,
    ) : GenericTypeProperty

    @Serializable
    class AssetOrArchiveProperty(
        @SerialName("\$ref")
        val ref: SpecificationReference,
        val willReplaceOnChanges: Boolean = false,
        override val description: String? = null,
        override val deprecationMessage: String? = null,
    ) : Property()

    @Serializable
    class ArchiveProperty(
        @SerialName("\$ref")
        val ref: SpecificationReference,
        val willReplaceOnChanges: Boolean = false,
        override val description: String? = null,
        override val deprecationMessage: String? = null,
    ) : Property()

    @Serializable
    data class ObjectProperty(
        val type: PropertyType = ObjectType,
        val properties: Map<PropertyName, Property> = emptyMap(),
        override val deprecationMessage: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val additionalProperties: Property? = null,
        val required: Set<PropertyName> = emptySet(),
        override val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : RootTypeProperty, ReferencingOtherTypesProperty

    @Serializable
    data class MapProperty(
        val type: PropertyType = ObjectType,
        override val deprecationMessage: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val additionalProperties: Property,
        override val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : GenericTypeProperty

    @Serializable(with = PropertySerializer::class)
    sealed interface Property {
        val description: String?
        val deprecationMessage: String?
    }

    @Serializable(with = PropertySerializer::class)
    sealed interface PrimitiveProperty : Property

    @Serializable(with = PropertySerializer::class)
    sealed interface ReferencingOtherTypesProperty : Property

    @Serializable(with = PropertySerializer::class)
    sealed interface GenericTypeProperty : ReferencingOtherTypesProperty

    @Serializable(with = PropertySerializer::class)
    sealed interface RootTypeProperty : Property

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
        val deprecationMessage: String? = null,
    )

    @Serializable
    data class Function(
        val inputs: ObjectProperty? = null,
        val outputs: ObjectProperty,
        val deprecationMessage: String? = null,
        val description: String? = null,
    )
}

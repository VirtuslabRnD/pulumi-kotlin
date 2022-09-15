package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

typealias TypesMap = Map<String, Resources.PropertySpecification>
typealias FunctionsMap = Map<String, Function>
typealias ResourcesMap = Map<String, Resources.Resource>

data class ParsedSchema(
    val types: TypesMap,
    val functions: FunctionsMap,
    val resources: ResourcesMap,
)

object Resources {

    object PropertySpecificationSerializer :
        JsonContentPolymorphicSerializer<PropertySpecification>(PropertySpecification::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out PropertySpecification> {
            fun hasTypeEqualTo(type: String) =
                element is JsonObject && "type" in element.jsonObject && element.jsonObject.getValue("type").jsonPrimitive.content == type

            fun isMapType() =
                element is JsonObject && "additionalProperties" in element.jsonObject && "properties" !in element.jsonObject

            fun mightBeOfTypeObject() =
                element is JsonObject && "properties" in element.jsonObject

            return when {
                element is JsonObject && "\$ref" in element.jsonObject -> ReferredProperty.serializer()
                element is JsonObject && "oneOf" in element.jsonObject -> OneOf.serializer()
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
    @Suppress(
        "ktlint:enum-entry-name-case",
        "EnumNaming",
    ) // this enum reflects the possible field values in the Pulumi schema
    enum class PropertyType {
        array, string, `object`, boolean, integer, number
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
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class StringProperty(
        val type: PropertyType,
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class BooleanProperty(
        val type: PropertyType,
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class IntegerProperty(
        val type: PropertyType,
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class NumberProperty(
        val type: PropertyType,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class ArrayProperty(
        val type: PropertyType,
        val items: PropertySpecification,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class ReferredProperty(
        val type: String? = null,
        val `$ref`: SpecificationReference,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class OneOf(
        val type: String? = null,
        val description: String? = null,
        val oneOf: List<PropertySpecification>,
        val deprecationMessage: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
        val willReplaceOnChanges: Boolean = false,
    ) : PropertySpecification()

    @Serializable
    data class ObjectProperty(
        val type: PropertyType = PropertyType.`object`,
        val properties: Map<PropertyName, PropertySpecification> = emptyMap(),
        val deprecationMessage: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val additionalProperties: PropertySpecification? = null,
        val required: Set<PropertyName> = emptySet(),
        val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable
    data class MapProperty(
        val type: PropertyType = PropertyType.`object`,
        val deprecationMessage: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val additionalProperties: PropertySpecification,
        val description: String? = null,
        val language: Language? = null,
        val default: JsonElement? = null,
    ) : PropertySpecification()

    @Serializable(with = PropertySpecificationSerializer::class)
    sealed class PropertySpecification

    @Serializable
    @JvmInline
    value class SpecificationReference(val value: String)

    @Serializable
    data class Resource(
        val description: String,
        val properties: Map<PropertyName, PropertySpecification> = emptyMap(),
        val type: PropertyType? = null,
        val required: List<PropertyName> = emptyList(),
        val inputProperties: Map<PropertyName, PropertySpecification> = emptyMap(),
        val requiredInputs: List<PropertyName> = emptyList(),
        val stateInputs: JsonObject? = null,
        val aliases: JsonArray? = null,
        val deprecationMessage: String? = null,
    )
}

@Serializable
data class Function(
    val inputs: Resources.ObjectProperty? = null,
    val outputs: Resources.ObjectProperty,
    val deprecationMessage: String? = null,
    val description: String? = null,
)

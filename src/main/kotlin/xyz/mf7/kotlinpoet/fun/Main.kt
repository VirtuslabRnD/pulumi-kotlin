package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

class Whatever {

}


data class Context(val valueMapKeys: List<String>)

//fun generate(parentKey: String?, map: Any?, context: Context): List<TypeSpec> {
//    val value = when(map) {
//        is Map<*, *> -> {
//            if(context.valueMapKeys.contains(parentKey)) {
//                map.map { (key, value) ->
//                    val newKey = key as String
//                    key to generate(key, value, context)
//                }
//            } else {
//
//            }
//        }
//
//        null -> {
//            null
//        }
//
//        is Int -> {
//
//        }
//
//        is Long -> {
//
//        }
//
//        is List<*> -> {
//
//        }
//
//        is String -> {
//
//        }
//
//        is Boolean -> {
//
//        }
//        else -> {
//            println("WARNING: unknown type $map")
//        }
//    }
//
//    if(context.valueMapKeys.contains(parentKey)) {
//
//    }
//

fun main() {

    val resource = Whatever::class.java.getResourceAsStream("/schema.json")!!

//    val json = ObjectMapper()

    val valuesForTheseKeysShouldBeAMap = listOf("types", "resources")

//    val schema = json.readValue<Map<String, Any>>(resource)

    val rootClass = ClassName("", "Root")

    val testClass = TypeSpec.classBuilder("Test")
        .addModifiers(KModifier.DATA)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("prop", String::class).build()
                )
                .build()
        )
        .addProperty(
            PropertySpec.builder("prop", String::class)
                .initializer("prop")
                .build()
        )
        .build()

    val otherType = TypeSpec.classBuilder("Test2")
        .addModifiers(KModifier.DATA)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("prop", ClassName("", testClass.name!!)).build()
                )
                .build()
        )
        .addProperty(
            PropertySpec.builder("prop", String::class)
                .initializer("prop")
                .build()
        )
        .build()

    val file = FileSpec.builder("", "file")
        .addType(testClass)
        .addType(otherType)
        .build()

    file.writeTo(System.out)

    val functionOne = Whatever::class.java.getResourceAsStream("/one-function-example.json")!!

    val functionFromJson = Json.decodeFromString<Function>(
        functionOne.bufferedReader().readText()
    )

    val resourceOne = Whatever::class.java.getResourceAsStream("/one-resource-example.json")!!

    val resourceFromJson = Json.decodeFromString<Resources.Resource>(
        resourceOne.bufferedReader().readText()
    )

    val loadedSchema = Whatever::class.java.getResourceAsStream("/schema.json")!!

    val schemaFromJson = Json.parseToJsonElement(
        loadedSchema.bufferedReader().readText()
    )

    val o =
        Json.decodeFromJsonElement<Map<String, Resources.PropertySpecification>>(schemaFromJson.jsonObject["types"]!!)

    println("asd")

}

object PropertySpecificationSerializer :
    JsonContentPolymorphicSerializer<PropertySpecification>(PropertySpecification::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "\$ref" in element.jsonObject -> ReferredProperty.serializer()
        "type" in element.jsonObject && element.jsonObject.getValue("type").jsonPrimitive.content == "array" -> ArrayProperty.serializer()
        "type" in element.jsonObject && element.jsonObject.getValue("type").jsonPrimitive.content == "string" -> StringProperty.serializer()
        else -> {
            StringProperty.serializer()
        }
    }
}


object Resources {

    object PropertySpecificationSerializer :
        JsonContentPolymorphicSerializer<PropertySpecification>(PropertySpecification::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out PropertySpecification> {
            fun hasTypeEqualTo(type: String) =
                element is JsonObject && "type" in element.jsonObject && element.jsonObject.getValue("type").jsonPrimitive.content == type
            return when {
                element is JsonObject &&  "\$ref" in element.jsonObject -> ReferredProperty.serializer()
                hasTypeEqualTo("array") -> ArrayProperty.serializer()
                hasTypeEqualTo("string") -> StringProperty.serializer()
                hasTypeEqualTo("object") -> ObjectProperty.serializer()
                hasTypeEqualTo("boolean") -> BooleanProperty.serializer()
                hasTypeEqualTo("integer") -> IntegerProperty.serializer()
                hasTypeEqualTo("number") -> NumberProperty.serializer()
                else -> {
                    error("Unknown ${element}")
                }
            }
        }
    }

    @Serializable
    @JvmInline
    value class Language(val map: Map<String, JsonElement>?)

    @Serializable
    enum class PropertyType {
        array, string, `object`, boolean, integer, number
    }

    @Serializable
    @JvmInline
    value class PropertyName(
        val value: String
    )

    @Serializable
    data class StringSingleEnum(val name: String, val value: String)

    @Serializable
    data class StringProperty(
        val type: PropertyType,
        val enum: List<StringSingleEnum> = emptyList(),
        val description: String? = null,
        val language: Language? = null
    ) :
        PropertySpecification()

    @Serializable
    data class BooleanProperty(
        val type: PropertyType,
        val description: String? = null,
        val language: Language? = null
    ) :
        PropertySpecification()

    @Serializable
    data class IntegerProperty(
        val type: PropertyType,
        val description: String? = null,
        val language: Language? = null
    ) :
        PropertySpecification()

    @Serializable
    data class NumberProperty(val type: PropertyType, val description: String? = null, val language: Language? = null) :
        PropertySpecification()

    @Serializable
    data class ArrayProperty(
        val type: PropertyType,
        val items: PropertySpecification,
        val description: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class ReferredProperty(
        val `$ref`: SpecificationReference,
        val description: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class ObjectProperty(
        val type: PropertyType,
        val properties: Map<PropertyName, PropertySpecification> = emptyMap(),
        val additionalProperties: Map<PropertyName, PropertySpecification> = emptyMap(),
        val required: List<PropertyName> = emptyList(),
        val description: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable(with = PropertySpecificationSerializer::class)
    sealed class PropertySpecification

    @Serializable
    @JvmInline
    value class SpecificationReference(val value: String)

    @Serializable
    data class Resource(
        val description: String,
        val properties: Map<PropertyName, PropertySpecification>,
        val type: PropertyType,
        val required: List<PropertyName>,
        val inputProperties: Map<PropertyName, PropertySpecification>
    )
}


@Serializable
enum class PropertyType {
    array, string, enum
}

@Serializable
data class StringProperty(val type: PropertyType, val description: String? = null) : PropertySpecification()

@Serializable
data class ArrayProperty(val type: PropertyType, val items: PropertySpecification, val description: String? = null) :
    PropertySpecification()

@Serializable
data class ReferredProperty(val `$ref`: SpecificationReference, val description: String? = null) :
    PropertySpecification()

@Serializable(with = PropertySpecificationSerializer::class)
sealed class PropertySpecification

@Serializable
@JvmInline
value class SpecificationReference(val value: String)

@Serializable
@JvmInline
value class PropertyName(
    val value: String
)

@Serializable
data class Inputs(
    val properties: Map<PropertyName, PropertySpecification>,
    val required: List<PropertyName>
)

@Serializable
data class Outputs(
    val properties: Map<PropertyName, PropertySpecification>
)

@Serializable
data class Function(
    val description: String,
    val inputs: Inputs,
    val outputs: Outputs
)

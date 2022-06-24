package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

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

typealias TypeMap = Map<String, Resources.PropertySpecification>

fun main() {

    val resource = Whatever::class.java.getResourceAsStream("/schema.json")!!

//    val json = ObjectMapper()

    val valuesForTheseKeysShouldBeAMap = listOf("types", "resources")

//    val schema = json.readValue<Map<String, Any>>(resource)

    val rootClass = ClassName("", "Root")

    val testClass = TypeSpec.classBuilder("Test").addModifiers(KModifier.DATA).primaryConstructor(
            FunSpec.constructorBuilder().addParameter(
                    ParameterSpec.builder("prop", String::class).build()
                ).build()
        ).addProperty(
            PropertySpec.builder("prop", String::class).initializer("prop").build()
        ).build()

    val otherType = TypeSpec.classBuilder("Test2").addModifiers(KModifier.DATA).primaryConstructor(
            FunSpec.constructorBuilder().addParameter(
                    ParameterSpec.builder("prop", ClassName("", testClass.name!!)).build()
                ).build()
        ).addProperty(
            PropertySpec.builder("prop", String::class).initializer("prop").build()
        ).build()

    val file = FileSpec.builder("", "file").addType(testClass).addType(otherType).build()

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

    val loadedSchemaClassic = Whatever::class.java.getResourceAsStream("/schema-aws-classic.json")!!

    val schemaFromJsonClassic = Json.parseToJsonElement(
        loadedSchemaClassic.bufferedReader().readText()
    )

    val typesForAwsNative = Json.decodeFromJsonElement<TypeMap>(schemaFromJson.jsonObject["types"]!!)


    val typesForAwsClassic = Json.decodeFromJsonElement<TypeMap>(schemaFromJsonClassic.jsonObject["types"]!!)

    generateKotlinCode(typesForAwsClassic).forEach {
        it.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/generated"))
    }

    generateKotlinCode(typesForAwsNative).forEach {
        it.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/generated"))
    }

    println("asd")

}

fun packageNameForName(name: String): String {
    return "xyz.mf7.generated." + name.replace("-", "").replace("/", "").split(":").dropLast(1).joinToString(".")
}

fun fileNameForName(name: String): String {
    return name.replace("-", "").replace("/", "").split(":").last()
}

fun classNameForName(name: String): ClassName {
    return ClassName(packageNameForName(name), fileNameForName(name))
}


/*
*
* {
*   "type": "object",
*   "properties": {
*       "apiGateway": {
*           "type": "object",
*           "name": {
*               "type": "string"
*           },
*           "url": {
*               "type": "string"
*           },
*           "lambdaReference": {
*               "type": "object",
*               "properties": {
*                   "arn": { "type": "string },
*                   "isProxied": { "type": "boolean" }
*               }
*           }
*       }
*   }
* }
*
* */

//fun evaluateInnerProperty(name: Resources.PropertyName?, property: Resources.PropertySpecification): PropertySpec {
//    val nameOrEmpty = name?.value ?: ""
//    when(property) {
//        is Resources.ArrayProperty -> {
//            val innerType = evaluateInnerProperty(null, property.items)
//            PropertySpec.builder(nameOrEmpty, ClassName("kotlin.collections", "List").parameterizedBy(innerType.type))
//                .addKdoc(property.description ?: "")
//                .build()
//        }
//
//        is Resources.BooleanProperty -> {
//            PropertySpec.builder(nameOrEmpty, Boolean::class)
//                .addKdoc(property.description ?: "")
//        }
//        is Resources.IntegerProperty -> {
//            PropertySpec.builder(nameOrEmpty, Integer::class)
//                .addKdoc(property.description ?: "")
//        }
//        is Resources.NumberProperty -> {
//            PropertySpec.builder(nameOrEmpty, Double::class)
//                .addKdoc(property.description ?: "")
//        }
//        is Resources.ObjectProperty -> {
//            val evaluated = property.properties.map { (innerName, innerSpec) ->
//                val e = evaluateInnerProperty(innerName, innerSpec)
//                if(isObject(e.type)) {
//
//                }
//            }
//
//            val innerType =
//                .addKdoc(property.description ?: "")
//                .build()
//        }
//        is Resources.OneOf -> TODO()
//        is Resources.ReferredProperty -> TODO()
//        is Resources.StringProperty -> TODO()
//    }
//}

fun referenceName(propertySpec: Resources.PropertySpecification): TypeName {
    return when (propertySpec) {
        is Resources.ArrayProperty -> ClassName(
            "kotlin.collections", "List"
        ).parameterizedBy(referenceName(propertySpec.items))
        is Resources.BooleanProperty -> ClassName("kotlin", "Boolean")
        is Resources.IntegerProperty -> ClassName("kotlin", "Long")
        is Resources.NumberProperty -> ClassName("kotlin", "Double")
        is Resources.ObjectProperty -> if (propertySpec.properties.isEmpty() && propertySpec.additionalProperties != null) {
            referenceName(propertySpec.additionalProperties)
        } else {
            error("deeply nested objects are not allowed (only maps are), description: ${propertySpec.description ?: "<null>"}")
        }
        is Resources.OneOf -> ClassName("kotlin", "Any")
        is Resources.ReferredProperty -> {
            val refTypeName = propertySpec.`$ref`.value
            if (refTypeName == "pulumi.json#/Any") {
                ClassName("kotlin", "Any")
            } else if (refTypeName.startsWith("#/types/")) {
                classNameForName(refTypeName.removePrefix("#/types/"))
            } else {
                error("type reference not recognized: $refTypeName")
            }
        }
        is Resources.StringProperty -> ClassName("kotlin", "String")
        is Resources.StringEnumProperty -> error("deeply nested enums are not allowed, description: ${propertySpec.description ?: "<null>"}")
    }
}

fun generateKotlinCode(typeMap: TypeMap): List<FileSpec> {
    return typeMap.map { (name, spec) ->
        val fileName = fileNameForName(name)
        val className = classNameForName(name)
        val packageName = packageNameForName(name)

        when (spec) {
            is Resources.ObjectProperty -> {
                val builder = FileSpec.builder(packageName, fileName)

                val classB = TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.DATA)

                val constructor = FunSpec.constructorBuilder()

                spec.properties.map { (innerPropertyName, innerPropertySpec) ->
                    val typeName = referenceName(innerPropertySpec).copy(nullable = !spec.required.contains(innerPropertyName))
                    classB
                        .addProperty(
                            PropertySpec
                                .builder(innerPropertyName.value, typeName)
                                .initializer(innerPropertyName.value)
                                .build()
                        )

                    constructor
                        .addParameter(innerPropertyName.value, typeName)
                }

                classB.primaryConstructor(constructor.build())

                builder.addType(classB.build()).build()
            }
            is Resources.StringEnumProperty -> {
                val builder = FileSpec.builder(packageName, fileName)

                val classB = TypeSpec.enumBuilder(className)
                    .primaryConstructor(
                        FunSpec.constructorBuilder().addParameter("value", String::class).build()
                    )
                    .addProperty(
                        PropertySpec.builder("value", String::class, KModifier.PRIVATE).initializer("value").build()
                    )

                spec.enum.forEach {
                    if (it.name == null || it.value == "*") {
                        println("WARN: ${it.name ?: "<null>"} ${it.value} encountered when handling the enum, skipping")
                    } else {
                        classB.addEnumConstant(
                            it.name,
                            TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", it.value).build()
                        )
                    }
                }

                builder.addType(classB.build()).build()
            }
            else -> error("unsupported")
        }
    }
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
                element is JsonObject && "\$ref" in element.jsonObject -> ReferredProperty.serializer()
                element is JsonObject && "oneOf" in element.jsonObject -> OneOf.serializer()
                hasTypeEqualTo("array") -> ArrayProperty.serializer()
                hasTypeEqualTo("string") && "enum" in element.jsonObject -> StringEnumProperty.serializer()
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
    data class StringSingleEnum(
        val name: String? = null,
        val value: String,
        val description: String? = null,
        val deprecationMessage: String? = null
    )

    @Serializable
    data class StringEnumProperty(
        val type: PropertyType,
        val enum: List<StringSingleEnum>,
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class StringProperty(
        val type: PropertyType,
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class BooleanProperty(
        val type: PropertyType,
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class IntegerProperty(
        val type: PropertyType,
        val description: String? = null,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class NumberProperty(
        val type: PropertyType,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val description: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class ArrayProperty(
        val type: PropertyType,
        val items: PropertySpecification,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val description: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class ReferredProperty(
        val type: String? = null,
        val `$ref`: SpecificationReference,
        val willReplaceOnChanges: Boolean = false,
        val deprecationMessage: String? = null,
        val description: String? = null,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class OneOf(
        val type: String? = null,
        val description: String? = null,
        val oneOf: List<PropertySpecification>,
        val language: Language? = null
    ) : PropertySpecification()

    @Serializable
    data class ObjectProperty(
        val type: PropertyType,
        val properties: Map<PropertyName, PropertySpecification> = emptyMap(),
        val willReplaceOnChanges: Boolean = false,
        val additionalProperties: PropertySpecification? = null,
        val required: Set<PropertyName> = emptySet(),
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
    val properties: Map<PropertyName, PropertySpecification>, val required: List<PropertyName>
)

@Serializable
data class Outputs(
    val properties: Map<PropertyName, PropertySpecification>
)

@Serializable
data class Function(
    val description: String, val inputs: Inputs, val outputs: Outputs
)

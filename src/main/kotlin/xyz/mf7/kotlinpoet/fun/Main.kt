package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

typealias TypeMap = Map<String, Resources.PropertySpecification>

fun main() {

    val loadedSchema = {}::class.java.getResourceAsStream("/schema.json")!!

    val schemaFromJson = Json.parseToJsonElement(
        loadedSchema.bufferedReader().readText()
    )

    val loadedSchemaClassic = { }::class.java.getResourceAsStream("/schema-aws-classic.json")!!

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
data class Inputs(
    val properties: Map<Resources.PropertyName, Resources.PropertySpecification>, val required: List<Resources.PropertyName>
)

@Serializable
data class Outputs(
    val properties: Map<Resources.PropertyName, Resources.PropertySpecification>
)

@Serializable
data class Function(
    val description: String, val inputs: Inputs, val outputs: Outputs
)

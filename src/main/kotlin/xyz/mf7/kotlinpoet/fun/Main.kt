package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.util.regex.Pattern

typealias TypeMap = Map<String, Resources.PropertySpecification>

typealias ResourcesMap = Map<String, Resources.Resource>

typealias FunctionsMap = Map<String, Function>


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

    val functionsForAwsClassic = Json.decodeFromJsonElement<FunctionsMap>(schemaFromJsonClassic.jsonObject["functions"]!!)

    generateTypes(typesForAwsClassic).forEach {
        it.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/generated"))
    }

    generateTypes(typesForAwsNative).forEach {
        it.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/generated"))
    }

    generateFunctions(functionsForAwsClassic).generatedFiles.forEach {
        it.writeTo(File("/Users/mfudala/workspace/kotlin-poet-fun/generated_functions"))
    }


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

data class GeneratedResource(
    val generatedFiles: List<FileSpec>,
    val identifiedOutputReferences: Set<Resources.PropertyName>,
    val identifiedInputReferences: Set<Resources.PropertyName>
)

data class GeneratedFunction(
    val generatedFiles: List<FileSpec>,
    val identifiedOutputReferences: Set<Resources.PropertyName>,
    val identifiedInputReferences: Set<Resources.PropertyName>
)

fun generateFunctions(functionsMap: FunctionsMap): GeneratedFunction {
    val files = functionsMap
        .entries
        .groupBy { (name, value) -> name.split("/").first() }
        .flatMap { (groupName, entries) ->

            val resultTypes = mutableListOf<FileSpec>()

            val file = FileSpec
                .builder(packageNameForName(groupName), fileNameForName(groupName).capitalize())

            entries.forEach { (name, function) ->

                val inputName = ClassName(packageNameForName(groupName), fileNameForName(groupName) + "Args")
                val outputName = ClassName(packageNameForName(groupName), fileNameForName(groupName) + "Result")

                function.inputs ?. let {
                    val i = constructDataClass(inputName, function.inputs)
                    val inputFile = FileSpec
                        .builder(packageNameForName(groupName), fileNameForName(groupName) + "Args")
                        .addType(i)
                        .build()

                    resultTypes.add(inputFile)
                }

                val o = constructDataClass(outputName, function.outputs)


                val outputFile = FileSpec
                    .builder(packageNameForName(groupName), fileNameForName(groupName) + "Result")
                    .addType(o)
                    .build()

                val realName = name.split(Regex("[/:]")).last()

                val funSpec = FunSpec
                    .builder(realName)
                    .let { f ->
                        function.description?.let {
                            f.addKdoc("Some kdoc was here but it does not work currently")
                        }
                        f
                    }
                    .addParameter("args", inputName)
                    .returns(outputName)
                    .build()

                file.addFunction(funSpec)

                resultTypes.add(outputFile)
            }

            resultTypes + listOf(file.build())
        }

    return GeneratedFunction(files, emptySet(), emptySet())
}

//fun generateResources(resourceMap: ResourcesMap): GeneratedResource {
//
//}

fun constructDataClass(className: ClassName, objectProperty: Resources.ObjectProperty): TypeSpec {
    val classB = TypeSpec.classBuilder(className)
        .addModifiers(KModifier.DATA)

    val constructor = FunSpec.constructorBuilder()

    objectProperty.properties.map { (innerPropertyName, innerPropertySpec) ->
        val typeName = referenceName(innerPropertySpec).copy(nullable = !objectProperty.required.contains(innerPropertyName))
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

    return classB.build()
}

fun generateTypes(typeMap: TypeMap): List<FileSpec> {
    return typeMap.map { (name, spec) ->
        val fileName = fileNameForName(name)
        val className = classNameForName(name)
        val packageName = packageNameForName(name)

        when (spec) {
            is Resources.ObjectProperty -> {
                val builder = FileSpec.builder(packageName, fileName)

                builder.addType(constructDataClass(className, spec)).build()
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

            fun mightBeOfTypeObject() =
                element is JsonObject && "properties" in element.jsonObject

            return when {
                element is JsonObject && "\$ref" in element.jsonObject -> ReferredProperty.serializer()
                element is JsonObject && "oneOf" in element.jsonObject -> OneOf.serializer()
                mightBeOfTypeObject() -> ObjectProperty.serializer()
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
        val type: PropertyType = PropertyType.`object`,
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
        val inputProperties: Map<PropertyName, PropertySpecification>,
        val requiredInputs: List<PropertyName>
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
    val inputs: Resources.ObjectProperty? = null, val outputs: Resources.ObjectProperty,
    val deprecationMessage: String? = null,
    val description: String? = null
)

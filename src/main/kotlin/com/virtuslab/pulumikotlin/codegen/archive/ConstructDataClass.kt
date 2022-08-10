package com.virtuslab.pulumikotlin.codegen.archive

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.virtuslab.pulumikotlin.codegen.MoreTypes
import com.virtuslab.pulumikotlin.codegen.classNameForNameSuffix
import com.virtuslab.pulumikotlin.codegen.letIf
import com.virtuslab.pulumikotlin.codegen.step1_schema_parse.Resources


fun constructDataClass(
    className: ClassName, objectProperty: Resources.ObjectProperty?,
    classModifier: (TypeSpec.Builder).() -> Unit = {},
    propertyModifier: (PropertySpec.Builder).(Resources.PropertyName, Resources.PropertySpecification, Boolean /*required?*/) -> Unit = { _, _, _ -> },
    shouldAddCustomTypeAnnotations: Boolean = false
): TypeSpec = constructDataClass(
    className,
    objectProperty?.properties,
    classModifier,
    propertyModifier,
    shouldAddCustomTypeAnnotations
)

fun constructDataClass(
    className: ClassName, properties: Map<Resources.PropertyName, Resources.PropertySpecification>?,
    classModifier: (TypeSpec.Builder).() -> Unit = {},
    propertyModifier: (PropertySpec.Builder).(Resources.PropertyName, Resources.PropertySpecification, Boolean /*required?*/) -> Unit = { _, _, _ -> },
    shouldAddCustomTypeAnnotations: Boolean = false,
    shouldWrapWithOutput: Boolean = false
): TypeSpec {
    val customTypeAnnotation = ClassName("com.pulumi.core.annotations", "CustomType")

    if (properties == null || properties.isEmpty()) {
        return TypeSpec.objectBuilder(className)
            .apply(classModifier)
            .build()
    }
    data class ParameterAndProperty(val parameter: ParameterSpec, val property: PropertySpec)

    val parameterAndProperties = properties.map { (innerPropertyName, innerPropertySpec) ->
        val isRequired = false /*objectProperty.required.contains(innerPropertyName)*/

        val typeName = referenceName(innerPropertySpec)
            .copy(nullable = !isRequired)
            .letIf(shouldWrapWithOutput) {
                val output: TypeName = MoreTypes.Java.Pulumi.Output(it.copy(nullable = false))
                output.letIf({ it.isNullable }) {
                    it.copy(nullable = true)
                }
            }

        val classProperty = PropertySpec.builder(innerPropertyName.value, typeName)
            .apply { this.propertyModifier(innerPropertyName, innerPropertySpec, isRequired) }
            .initializer(innerPropertyName.value)
            .build()

        val constructorParameter = ParameterSpec.builder(innerPropertyName.value, typeName)
            .letIf(!isRequired) {
                it.defaultValue("%L", null)
            }
            .letIf(shouldAddCustomTypeAnnotations) {
                it.addAnnotation(
                    AnnotationSpec.builder(customTypeAnnotation.nestedClass("Parameter"))
                        .addMember("%S", innerPropertyName.value)
                        .build()
                )
            }
            .build()

        ParameterAndProperty(constructorParameter, classProperty)
    }

    val constructor = FunSpec.constructorBuilder()
        .letIf(shouldAddCustomTypeAnnotations) {
            it.addAnnotation(customTypeAnnotation.nestedClass("Constructor"))
        }
        .addParameters(parameterAndProperties.map { it.parameter })
        .build()

    return TypeSpec.classBuilder(className)
        .letIf(shouldAddCustomTypeAnnotations) {
            it.addAnnotation(customTypeAnnotation)
        }
        .addModifiers(KModifier.DATA)
        .primaryConstructor(constructor)
        .apply(classModifier)
        .build()
}

enum class Language {
    KOTLIN, JAVA
}

fun referenceName(
    propertySpec: Resources.PropertySpecification,
    suffix: String = "",
    language: Language = Language.KOTLIN
): TypeName {
    return when (propertySpec) {
        is Resources.ArrayProperty -> LIST.parameterizedBy(referenceName(propertySpec.items, suffix))
        is Resources.BooleanProperty -> BOOLEAN
        is Resources.IntegerProperty -> INT
        is Resources.NumberProperty -> DOUBLE
        is Resources.OneOf -> ANY
        is Resources.StringProperty -> STRING
        is Resources.MapProperty -> MAP.parameterizedBy(
            STRING,
            referenceName(propertySpec.additionalProperties, suffix)
        )

        is Resources.ObjectProperty -> if (propertySpec.properties.isEmpty() && propertySpec.additionalProperties != null) {
            referenceName(propertySpec.additionalProperties)
        } else {
            error("deeply nested objects are not allowed (only maps are), description: ${propertySpec.description ?: "<null>"}")
        }
        is Resources.ReferredProperty -> {
            val refTypeName = propertySpec.`$ref`.value
            if (refTypeName == "pulumi.json#/Any") {
                ClassName("kotlin", "Any")
            } else if (refTypeName.startsWith("#/types/")) {
                when (language) {
                    Language.KOTLIN -> classNameForNameSuffix(refTypeName.removePrefix("#/types/"), suffix)
                    Language.JAVA -> classNameForNameSuffix(refTypeName.removePrefix("#/types/"), suffix)
                }

            } else if (refTypeName == "pulumi.json#/Archive") {
                ClassName("kotlin", "Any") // TODO: this should be archive
            } else if (refTypeName == "pulumi.json#/Asset") {
                ClassName("kotlin", "Any") // TODO: this should be archive
            } else {
                error("type reference not recognized: $refTypeName")
            }
        }
        is Resources.StringEnumProperty -> error("deeply nested enums are not allowed, description: ${propertySpec.description ?: "<null>"}")
    }
}

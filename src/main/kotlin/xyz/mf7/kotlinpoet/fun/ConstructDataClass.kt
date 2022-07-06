package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun constructDataClass(
    className: ClassName, objectProperty: Resources.ObjectProperty?,
    classModifier: (TypeSpec.Builder).() -> Unit = {},
    propertyModifier: (PropertySpec.Builder).(Resources.PropertyName, Resources.PropertySpecification, Boolean /*required?*/) -> Unit = { _, _, _ -> },
    shouldAddCustomTypeAnnotations: Boolean = false
): TypeSpec {
    val customTypeAnnotation = ClassName("com.pulumi.core.annotations", "CustomType")

    val classB = TypeSpec.classBuilder(className)
        .let {
            if(shouldAddCustomTypeAnnotations) {
                it.addAnnotation(customTypeAnnotation)
            } else {
                it
            }
        }
        .addModifiers(KModifier.DATA)
        .apply(classModifier)

    val constructor = FunSpec.constructorBuilder()
        .let {
            if(shouldAddCustomTypeAnnotations) {
                it.addAnnotation(customTypeAnnotation.nestedClass("Constructor"))
            } else {
                it
            }
        }

    if (objectProperty == null || objectProperty.properties.isEmpty()) {
        return TypeSpec.objectBuilder(className)
            .apply(classModifier)
            .build()
    }

    objectProperty.properties.map { (innerPropertyName, innerPropertySpec) ->
        val isRequired = false /*objectProperty.required.contains(innerPropertyName)*/
        if(className.simpleName.endsWith("FunctionResult") && className.packageName.contains("lambda")) {
            println("debug")
        }
        val typeName = referenceName(innerPropertySpec).copy(nullable = !isRequired)
        classB
            .addProperty(
                PropertySpec.builder(innerPropertyName.value, typeName)
                    .apply { this.propertyModifier(innerPropertyName, innerPropertySpec, isRequired) }
                    .initializer(innerPropertyName.value)
                    .build()
            )

        constructor
            .addParameter(
                ParameterSpec.builder(innerPropertyName.value, typeName)
                    .let {
                        if (!isRequired) {
                            it.defaultValue("%L", null)
                        } else {
                            it
                        }
                    }.let {
                        if(shouldAddCustomTypeAnnotations) {
                            it.addAnnotation(
                                AnnotationSpec.builder(customTypeAnnotation.nestedClass("Parameter"))
                                    .addMember("%S", innerPropertyName.value)
                                    .build()
                            )
                        } else {
                            it
                        }
                    }
                    .build()
            )
    }

    classB.primaryConstructor(constructor.build())

    return classB.build()
}

fun referenceName(propertySpec: Resources.PropertySpecification): TypeName {
    return when (propertySpec) {
        is Resources.ArrayProperty -> LIST.parameterizedBy(referenceName(propertySpec.items))
        is Resources.BooleanProperty -> BOOLEAN
        is Resources.IntegerProperty -> INT
        is Resources.NumberProperty -> DOUBLE
        is Resources.OneOf -> ANY
        is Resources.StringProperty -> STRING
        is Resources.MapProperty -> MAP.parameterizedBy(STRING, referenceName(propertySpec.additionalProperties))

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
                classNameForName(refTypeName.removePrefix("#/types/"))
            } else {
                error("type reference not recognized: $refTypeName")
            }
        }
        is Resources.StringEnumProperty -> error("deeply nested enums are not allowed, description: ${propertySpec.description ?: "<null>"}")
    }
}
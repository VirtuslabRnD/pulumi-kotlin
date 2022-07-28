package com.virtuslab.pulumikotlin.codegen

import com.squareup.kotlinpoet.*

fun generateType(type: ComplexType): FileSpec {
//    val fields = type.fields.map { (key, value) ->
//        Field(key, value.toTypeName(), false, emptyList())
//    }
//
//    val b = generateTypeWithNiceBuilders(
//        type.metadata.toClassName(LanguageType.Kotlin),
//        type.metadata.toPackage(LanguageType.Kotlin),
//        type.metadata.toClassName(LanguageType.Kotlin),
//        type.metadata.toClassName(LanguageType.Kotlin).decapitalize(),
//        type.metadata.toClassName(LanguageType.Kotlin) + "Builder",
//        type.parent.toTypeName(),
//        shouldJustReturn = false,
//        receiverClassNameForBuilderMethod = type.parent.toTypeName(),
//        fields
//    )
//
//    return b

    return FileSpec.builder("a", "b").build()
}

fun generateTypes2(typesWithMetadata: List<TypeWithMetadata>): List<FileSpec> {
    return typesWithMetadata.mapNotNull { type ->
        when(val o = type.type) {
            is ComplexType -> generateType(o)
            else -> null
        }
    }
}
fun generateTypes(typesMap: TypesMap): List<FileSpec> {
    return typesMap.map { (name, spec) ->
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
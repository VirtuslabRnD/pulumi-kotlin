package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.*

fun generateTypes2(typesWithMetadata: List<TypeWithMetadata>): List<FileSpec> {
    return typesWithMetadata.map { type ->
        val typeSpec = type.type
        generateTypeWithNiceBuilders(
            type.names.typeClassName,
            type.names.packageName,
            type.names.typeClassName,
            type.names.typeBuilderClassName,
            type.names.functionBuilderClassName,
            "args",
            ClassName("a", "b"),
            when(typeSpec) {
                is Resources.ObjectProperty -> {
                    typeSpec.properties.map { (name, spec) ->
                        Field(name.value, referenceName(spec), false, emptyList())
                    }
                }
                else -> emptyList()
            }
        )
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
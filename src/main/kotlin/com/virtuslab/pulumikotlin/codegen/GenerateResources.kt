package com.virtuslab.pulumikotlin.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

object PulumiClassesAndMembers {
    val output = MoreTypes.Java.Pulumi.Output()
    val outputOf = output.member("of")
}

fun buildArgsClass(fileSpecBuilder: FileSpec.Builder, name: String, spec: Resources.Resource) {
    val dslTag = ClassName("com.pulumi.kotlin", "PulumiTagMarker")

    val customArgs = ClassName("com.pulumi.kotlin", "CustomArgs")

    val resourceClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "Resource")

    val javaResourceClassName = ClassName(javaPackageNameForName(name), fileNameForName(name))

    val javaResourceArgsBuilderClassName =
        ClassName(javaPackageNameForName(name), fileNameForName(name) + "Args")

    val resourceClass = TypeSpec
        .classBuilder(resourceClassName)
        .addProperties(
            listOf(
                PropertySpec.builder("javaResource", javaResourceClassName)
                    .initializer("javaResource")
                    .build()
            )
        )
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("javaResource", javaResourceClassName)
                .build()
        )
        .build()

    val resourceBuilderClassName =
        ClassName(resourcePackageNameForName(name), fileNameForName(name) + "ResourceBuilder")

    val argsClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "Args")

//    val niceBuilderFileSpec = generateTypeWithNiceBuilders(
//        fileNameForName(name),
//        resourcePackageNameForName(name), fileNameForName(name) + "Args",
//        fileNameForName(name) + "ArgsBuilder",
//        "args",
//        "args",
//        resourceBuilderClassName,
//        spec.inputProperties.map {
//            val typeName = referenceName(it.value)
//            val outputWrappedTypeName = PulumiClassesAndMembers.output.parameterizedBy(typeName)
//            val nonOutputOverload = FieldOverload(
//                typeName,
//                { from, to -> CodeBlock.of("val %N = %T.%M(%N)", to, PulumiClassesAndMembers.output, PulumiClassesAndMembers.outputOf, from) }
//            )
//            Field(it.key.value, outputWrappedTypeName, required = false, listOf(nonOutputOverload))
//        }
//    )

    val resourceBuilderClass = TypeSpec
        .classBuilder(resourceBuilderClassName)
        .addAnnotation(dslTag)
        .addProperties(
            listOf(
                PropertySpec.builder("name", STRING.copy(nullable = true))
                    .mutable(true)
                    .initializer("null")
                    .build(),
                PropertySpec.builder("args", argsClassName.copy(nullable = true))
                    .mutable(true)
                    .initializer("null")
                    .build(),
                PropertySpec.builder("opts", customArgs)
                    .mutable(true)
                    .initializer("%T()", customArgs)
                    .build()
            )
        )
        .addFunction(
            FunSpec.builder("name")
                .addParameter("value", STRING)
                .addCode("this.name = value")
                .build()
        )
        .addFunction(
            FunSpec.builder("build")
                .let {
                    val inputProperties = spec.inputProperties.keys.toList()
                    val builderInvocations = inputProperties
                        .map { name ->
                            ".%N(%T.%M<%T>(this.args?.%N))"
                        }
                        .joinToString("\n")

                    it.addCode(
                        """
                        val builtJavaResource = %T(
                            this.name,
                            %T.%N()${builderInvocations}.%N(),
                            this.opts.toJava()
                        )
                        """,
                        *arrayOf(
                            javaResourceClassName,
                            javaResourceArgsBuilderClassName,
                            "builder",
                            *spec.inputProperties.flatMap { (pName, pSpec) ->
                                listOf(
                                    pName.value,
                                    ClassName("com.pulumi.kotlin", "PulumiJavaKotlinInterop"),
                                    ClassName("com.pulumi.kotlin", "PulumiJavaKotlinInterop").member("toJava"),
                                    MoreTypes.Java.Pulumi.Output(referenceName(
                                        pSpec,
                                        suffix = "Args"
                                    )),
                                    pName.value
                                )
                            }.toTypedArray(),
                            "build"
                        )
                    )

                    it.addCode("return %T(builtJavaResource)", resourceClassName)
                }
                .returns(resourceClassName)
                .build(),
        )
        .build()



    val optsFunction = FunSpec
        .builder("opts")
        .receiver(resourceBuilderClassName)
        .addModifiers(KModifier.SUSPEND)
        .addParameter(
            "block", LambdaTypeName.get(
                ClassName("com.pulumi.kotlin", "CustomArgsBuilder"),
                returnType = UNIT,
            ).copy(suspending = true)
        )
        .addStatement("val builder = %T()", ClassName("com.pulumi.kotlin", "CustomArgsBuilder"))
        .addStatement("block(builder)")
        .addStatement("this.opts = builder.build()")
        .build()

    val resourceFunction = FunSpec
        .builder(fileNameForName(name).decapitalize() + "Resource")
        .addModifiers(KModifier.SUSPEND)
        .returns(resourceClassName)
        .addParameter(
            "block", LambdaTypeName.get(
                resourceBuilderClassName,
                returnType = UNIT
            ).copy(suspending = true)
        )
        .addStatement("val builder = %T()", resourceBuilderClassName)
        .addStatement("block(builder)")
        .addStatement("return builder.build()")
        .build()

    fileSpecBuilder
        .addType(resourceBuilderClass)
        .addType(resourceClass)
        .addFunction(optsFunction)
        .addFunction(resourceFunction)
}

private fun generateFunctionsForInput(
    name: Resources.PropertyName,
    spec: Resources.PropertySpecification,
): List<FunSpec> {
    return buildList {
        val ref = referenceName(spec)
        add(
            FunSpec
                .builder(name.value)
                .addParameter("value", PulumiClassesAndMembers.output.parameterizedBy(ref).copy(nullable = true))
                .addCode("this.${name.value} = value")
                .build()
        )
        add(
            FunSpec
                .builder(name.value)
                .addParameter("value", ref.copy(nullable = true))
                .addCode(
                    "this.${name.value} = value?.let { %T.%M(value) }",
                    PulumiClassesAndMembers.output,
                    PulumiClassesAndMembers.outputOf
                )
                .build()
        )
        if (ref is ParameterizedTypeName) {
            when (ref.rawType) {
                LIST ->
                    add(
                        FunSpec
                            .builder(name.value)
                            .addParameter("values", ref.typeArguments.get(0), KModifier.VARARG)
                            .addCode(
                                "this.${name.value} = values.toList().let { %T.%M(it) }",
                                PulumiClassesAndMembers.output,
                                PulumiClassesAndMembers.outputOf
                            )
                            .build()
                    )
                MAP ->
                    add(
                        FunSpec
                            .builder(name.value)
                            .addParameter(
                                "values",
                                ClassName("kotlin", "Pair").parameterizedBy(
                                    ref.typeArguments.get(0),
                                    ref.typeArguments.get(1)
                                ),
                                KModifier.VARARG
                            )
                            .addCode(
                                "this.${name.value} = values.toList().toMap().let { %T.%M(it) }",
                                PulumiClassesAndMembers.output,
                                PulumiClassesAndMembers.outputOf
                            )
                            .build()
                    )
            }
        }
    }
}


fun generateResources(resourcesMap: ResourcesMap): GeneratedResources {
    val files = resourcesMap.entries.map { (name, spec) ->

        val file = FileSpec.builder(resourcePackageNameForName(name), fileNameForName(name).capitalize())

        buildArgsClass(file, name, spec)
        file.build()
    }

    return GeneratedResources(files, emptySet(), emptySet())
}

data class GeneratedResources(
    val generatedFiles: List<FileSpec>,
    val identifiedOutputReferences: Set<Resources.PropertyName>,
    val identifiedInputReferences: Set<Resources.PropertyName>
)

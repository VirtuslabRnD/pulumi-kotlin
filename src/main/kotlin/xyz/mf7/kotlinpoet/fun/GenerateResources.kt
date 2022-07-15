package xyz.mf7.kotlinpoet.`fun`

import com.pulumi.aws.apigatewayv2.inputs.AuthorizerJwtConfigurationArgs
import com.pulumi.core.Output
import com.pulumi.kotlin.PulumiJavaKotlinInterop
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy


fun buildArgsClass(fileSpecBuilder: FileSpec.Builder, name: String, spec: Resources.Resource) {
    val dslTag = ClassName("com.pulumi.kotlin", "PulumiTagMarker")
    val output = ClassName("com.pulumi.core", "Output")

    val customArgs = ClassName("com.pulumi.kotlin", "CustomArgs")

    val outputOf = output.member("of")


    val argsClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "Args")
    val argsBuilderClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "ArgsBuilder")

    val argsClass = constructDataClass(argsClassName, spec.inputProperties, shouldWrapWithOutput = true)

    val argNames = spec.inputProperties.keys.map {
        "${it.value} = ${it.value}"
    }.joinToString(", ")

    val argsBuilderClass = TypeSpec
        .classBuilder(argsBuilderClassName)
        .addAnnotation(dslTag)
        .addProperties(
            spec.inputProperties.map {
                PropertySpec
                    .builder(it.key.value, output.parameterizedBy(referenceName(it.value)).copy(nullable = true))
                    .initializer("null")
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            }
        )
        .addFunctions(
            spec.inputProperties.flatMap {
                buildList<FunSpec> {
                    val ref = referenceName(it.value)
                    add(
                        FunSpec
                            .builder(it.key.value)
                            .addParameter("value", output.parameterizedBy(ref).copy(nullable = true))
                            .addCode("this.${it.key.value} = value")
                            .build()
                    )
                    add(
                        FunSpec
                            .builder(it.key.value)
                            .addParameter("value", ref.copy(nullable = true))
                            .addCode("this.${it.key.value} = value?.let { %T.%M(value) }", output, outputOf)
                            .build()
                    )
                    if (ref is ParameterizedTypeName) {
                        if (ref.rawType == LIST) {
                            add(
                                FunSpec
                                    .builder(it.key.value)
                                    .addParameter("values", ref.typeArguments.get(0), KModifier.VARARG)
                                    .addCode(
                                        "this.${it.key.value} = values.toList().let { %T.%M(it) }",
                                        output,
                                        outputOf
                                    )
                                    .build()
                            )
                        } else if (ref.rawType == MAP) {
                            add(
                                FunSpec
                                    .builder(it.key.value)
                                    .addParameter(
                                        "values",
                                        ClassName("kotlin", "Pair").parameterizedBy(
                                            ref.typeArguments.get(0),
                                            ref.typeArguments.get(1)
                                        ),
                                        KModifier.VARARG
                                    )
                                    .addCode(
                                        "this.${it.key.value} = values.toList().toMap().let { %T.%M(it) }",
                                        output,
                                        outputOf
                                    )
                                    .build()
                            )
                        }
                    }
                }
            }
        )
        .addFunction(
            FunSpec.builder("build")
                .returns(argsClassName)
                .addCode("return %T(${argNames})", argsClassName)
                .build()
        )
        .build()

    val resourceClassName = ClassName(resourcePackageNameForName(name), fileNameForName(name) + "Resource")

    val javaResourceClassName = ClassName(javaPackageNameForName(name), fileNameForName(name))

    val javaResourceArgsBuilderClassName =
        ClassName(javaPackageNameForName(name), fileNameForName(name) + "Args")

    val resourceClass = TypeSpec
        .classBuilder(resourceClassName)
        .addProperties(
            listOf(
                PropertySpec.builder("javaResource", javaResourceClassName)
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
                            *spec.inputProperties.flatMap {(pName, pSpec) ->
                                listOf(
                                    pName.value,
                                    ClassName("com.pulumi.kotlin", "PulumiJavaKotlinInterop"),
                                    ClassName("com.pulumi.kotlin", "PulumiJavaKotlinInterop").member("toJava"),
                                    ClassName("com.pulumi.core", "Output").parameterizedBy(referenceName(pSpec, suffix = "Args")),
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

    val argsFunction = FunSpec
        .builder("args")
        .receiver(resourceBuilderClassName)
        .addModifiers(KModifier.SUSPEND)
        .addParameter(
            "block", LambdaTypeName.get(
                argsBuilderClassName,
                returnType = UNIT
            ).copy(suspending = true)
        )
        .addStatement("val builder = %T()", argsBuilderClassName)
        .addStatement("block(builder)")
        .addStatement("this.args = builder.build()")
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
        .addParameter(
            "block", LambdaTypeName.get(
                resourceBuilderClassName,
                returnType = UNIT
            ).copy(suspending = true)
        )
        .addStatement("val builder = %T()", resourceBuilderClassName)
        .addStatement("block(builder)")
        .addStatement("builder.build()")
        .build()

    fileSpecBuilder
        .addType(argsClass)
        .addType(argsBuilderClass)
        .addType(resourceBuilderClass)
        .addType(resourceClass)
        .addFunction(argsFunction)
        .addFunction(optsFunction)
        .addFunction(resourceFunction)
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

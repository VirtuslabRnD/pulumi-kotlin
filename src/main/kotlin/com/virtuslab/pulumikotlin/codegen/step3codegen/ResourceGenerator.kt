package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.virtuslab.pulumikotlin.codegen.expressions.*
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.Resources
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*

object ResourceGenerator {
    fun generateResources(resources: List<ResourceType>): List<FileSpec> {
        val files = resources.map { type ->
            val file = FileSpec.builder(type.name.toResourcePackage(NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)), type.name.toResourceName(NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)) + ".kt")

            buildArgsClass(file, type)

            file.build()
        }

        return files
    }

    private fun toKotlinExpressionResource(expression: Expression, type: Type, optional: Boolean = false): Expression {
        return when (type) {
            AnyType -> expression
            is ComplexType -> expression.callLet(optional) { argument -> type.toTypeName().nestedClass("Companion").member("toKotlin")(argument) }
            is EnumType -> expression.callLet(optional) { argument -> type.toTypeName().nestedClass("Companion").member("toKotlin")(argument) }
            is EitherType -> expression
            is ListType -> expression.callMap(optional) { argument -> toKotlinExpressionResource(argument, type.innerType) }
            is MapType ->
                expression
                    .callMap(optional) { argument -> argument.field("key").pairWith(toKotlinExpressionResource(argument.field("value"), type.secondType)) }
                    .call0("toMap", optional)

            is PrimitiveType -> expression
        }
    }

    private fun toKotlinExpressionBaseResource(name: String): Expression {
        return CustomExpression("javaResource.%N()", KeywordsEscaper.escape(name))
    }

    private fun toKotlinFunctionResource(name: String, type: Type, optional: Boolean): Code {
        val baseE = toKotlinExpressionBaseResource(name)
        val secondPart = baseE.callApplyValue { arg -> toKotlinExpressionResource(arg.call0("toKotlin", optional), type, optional) }

        return Return(secondPart)
    }

    private fun buildArgsClass(fileSpecBuilder: FileSpec.Builder, resourceType: ResourceType) {
        val dslTag = ClassName("com.pulumi.kotlin", "PulumiTagMarker")

        val customArgs = ClassName("com.pulumi.kotlin", "CustomArgs")

        val javaFlags = NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceRoot, LanguageType.Java)
        val kotlinFlags = NamingFlags(InputOrOutput.Output, UseCharacteristic.ResourceRoot, LanguageType.Kotlin)

        val names = resourceType.name
        val resourceClassName = ClassName(names.toResourcePackage(kotlinFlags), names.toResourceName(kotlinFlags))
        val javaResourceClassName = ClassName(names.toResourcePackage(javaFlags), names.toResourceName(javaFlags))

        val fields = resourceType.outputFields.map { field ->
            PropertySpec.builder(field.name, MoreTypes.Java.Pulumi.Output(field.fieldType.type.toTypeName().copy(nullable = !field.required)))
                .getter(FunSpec.getterBuilder().addCode(toKotlinFunctionResource(field.name, field.fieldType.type, !field.required)).build())
                .build()
        }


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
            .addProperties(fields)
            .build()

        val resourceBuilderClassName =
            ClassName(names.toResourcePackage(kotlinFlags), names.toResourceName(kotlinFlags) + "ResourceBuilder")

        val argsClassName = resourceType.argsType.toTypeName()
        val argsBuilderClassName = (resourceType.argsType as ComplexType).toBuilderTypeName()

        val argsFunction = FunSpec
            .builder("args")
            .addModifiers(KModifier.SUSPEND)
            .addParameter(
                "block", LambdaTypeName.get(
                    argsBuilderClassName,
                    returnType = UNIT,
                ).copy(suspending = true)
            )
            .addStatement("val builder = %T()", argsBuilderClassName)
            .addStatement("block(builder)")
            .addStatement("this.args = builder.build()")
            .build()


        val optsFunction = FunSpec
            .builder("opts")
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
            .addFunction(argsFunction)
            .addFunction(optsFunction)
            .addFunction(
                FunSpec.builder("build")
                    .let {
                        it.addCode(
                            """
                        val builtJavaResource = %T(
                            this.name,
                            this.args!!.toJava(),
                            this.opts.toJava()
                        )
                        """,
                            javaResourceClassName
                        )

                        it.addCode("return %T(builtJavaResource)", resourceClassName)
                    }
                    .returns(resourceClassName)
                    .build(),
            )
            .build()

        val resourceFunction = FunSpec
            .builder(names.toResourceName(kotlinFlags).decapitalize() + "Resource")
            .addModifiers(KModifier.SUSPEND)
            .returns(resourceClassName)
            .addParameter("name", STRING)
            .addParameter(
                "block", LambdaTypeName.get(
                    resourceBuilderClassName,
                    returnType = UNIT
                ).copy(suspending = true)
            )
            .addStatement("val builder = %T()", resourceBuilderClassName)
            .addStatement("builder.name(name)")
            .addStatement("block(builder)")
            .addStatement("return builder.build()")
            .build()

        fileSpecBuilder
            .addType(resourceBuilderClass)
            .addType(resourceClass)
            .addImport("com.pulumi.kotlin", "toKotlin")
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

    private enum class Language {
        KOTLIN, JAVA
    }

    private fun referenceName(
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

    private fun classNameForNameSuffix(name: String, suffix: String): ClassName {
        return ClassName(packageNameForName(name), fileNameForName(name) + suffix)
    }

    private fun fileNameForName(name: String): String {
        return name.split("/").last().split(":").last().replace("-", "").capitalize()
    }

    private fun packageNameForName(name: String): String {
        return "com.pulumi.kotlin." + name.split("/").first().replace(":", ".").replace("-", "")
    }

    private object PulumiClassesAndMembers {
        val output = MoreTypes.Java.Pulumi.Output()
        val outputOf = output.member("of")
    }
}

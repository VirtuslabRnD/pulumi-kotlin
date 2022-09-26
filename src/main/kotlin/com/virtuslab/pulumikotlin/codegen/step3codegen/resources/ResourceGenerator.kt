package com.virtuslab.pulumikotlin.codegen.step3codegen.resources

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NamingFlags
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic
import com.virtuslab.pulumikotlin.codegen.step3codegen.resources.ToKotlin.toKotlinFunctionResource
import com.virtuslab.pulumikotlin.codegen.utils.decapitalize

object ResourceGenerator {
    fun generateResources(resources: List<ResourceType>): List<FileSpec> {
        val files = resources.map { type ->
            val file = FileSpec.builder(
                type.name.toResourcePackage(
                    NamingFlags(
                        InputOrOutput.Output,
                        UseCharacteristic.ResourceRoot,
                        LanguageType.Kotlin,
                    ),
                ),
                type.name.toResourceName(
                    NamingFlags(
                        InputOrOutput.Output,
                        UseCharacteristic.ResourceRoot,
                        LanguageType.Kotlin,
                    ),
                ),
            )

            buildArgsClass(file, type)

            file.build()
        }

        return files
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
            PropertySpec.builder(
                field.name,
                MoreTypes.Java.Pulumi.Output(
                    field.fieldType.type.toTypeName().copy(nullable = !field.required),
                ),
            )
                .getter(
                    FunSpec.getterBuilder()
                        .addCode(toKotlinFunctionResource(field.name, field.fieldType.type, field.required)).build(),
                )
                .build()
        }

        val resourceClass = TypeSpec
            .classBuilder(resourceClassName)
            .addProperties(
                listOf(
                    PropertySpec.builder("javaResource", javaResourceClassName)
                        .initializer("javaResource")
                        .build(),
                ),
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("javaResource", javaResourceClassName)
                    .build(),
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
                "block",
                LambdaTypeName.get(
                    argsBuilderClassName,
                    returnType = UNIT,
                ).copy(suspending = true),
            )
            .addStatement("val builder = %T()", argsBuilderClassName)
            .addStatement("block(builder)")
            .addStatement("this.args = builder.build()")
            .build()

        val optsFunction = FunSpec
            .builder("opts")
            .addModifiers(KModifier.SUSPEND)
            .addParameter(
                "block",
                LambdaTypeName.get(
                    ClassName("com.pulumi.kotlin", "CustomArgsBuilder"),
                    returnType = UNIT,
                ).copy(suspending = true),
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
                        .build(),
                ),
            )
            .addFunction(
                FunSpec.builder("name")
                    .addParameter("value", STRING)
                    .addCode("this.name = value")
                    .build(),
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
                            javaResourceClassName,
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
                "block",
                LambdaTypeName.get(
                    resourceBuilderClassName,
                    returnType = UNIT,
                ).copy(suspending = true),
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
}

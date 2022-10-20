package com.virtuslab.pulumikotlin.codegen.step3codegen.resources

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.customArgsBuilderClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.customArgsClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.pulumiDslMarkerAnnotation
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NamingFlags
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDeprecationWarningIfAvailable
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDocs
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDocsIfAvailable
import com.virtuslab.pulumikotlin.codegen.step3codegen.resources.ToKotlin.toKotlinFunctionResource
import com.virtuslab.pulumikotlin.codegen.utils.decapitalize

object ResourceGenerator {
    fun generateResources(resources: List<ResourceType>): List<FileSpec> {
        val files = resources.map { type ->
            val file = FileSpec.builder(
                type.name.toResourcePackage(NamingFlags(Root, Resource, Output, Kotlin)),
                type.name.toResourceName(NamingFlags(Root, Resource, Output, Kotlin)),
            )

            buildArgsClass(file, type)

            file.build()
        }

        return files
    }

    private fun buildArgsClass(fileSpecBuilder: FileSpec.Builder, resourceType: ResourceType) {
        val javaFlags = NamingFlags(Root, Resource, Output, Java)
        val kotlinFlags = NamingFlags(Root, Resource, Output, Kotlin)

        val names = resourceType.name
        val resourceClassName = ClassName(names.toResourcePackage(kotlinFlags), names.toResourceName(kotlinFlags))
        val javaResourceClassName = ClassName(names.toResourcePackage(javaFlags), names.toResourceName(javaFlags))

        val fields = resourceType.outputFields.map { field ->
            PropertySpec
                .builder(field.name, field.toTypeName())
                .getter(
                    FunSpec.getterBuilder()
                        .addCode(toKotlinFunctionResource(field.name, field.fieldType.type, !field.required))
                        .build(),
                )
                .addDocsIfAvailable(field.kDoc)
                .addDeprecationWarningIfAvailable(field.kDoc)
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
                    .addModifiers(INTERNAL)
                    .addParameter("javaResource", javaResourceClassName)
                    .build(),
            )
            .addProperties(fields)
            .addDocsIfAvailable(resourceType.kDoc)
            .addDeprecationWarningIfAvailable(resourceType.kDoc)
            .build()

        val resourceBuilderClassName = ClassName(
            names.toResourcePackage(kotlinFlags),
            names.toResourceName(kotlinFlags) + "ResourceBuilder",
        )

        val argsClassName = resourceType.argsType.toTypeName()
        val argsBuilderClassName = resourceType.argsType.toBuilderTypeName()

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
            .addDocs("@param block The arguments to use to populate this resource's properties.")
            .build()

        val optsFunction = FunSpec
            .builder("opts")
            .addModifiers(KModifier.SUSPEND)
            .addParameter(
                "block",
                LambdaTypeName.get(
                    customArgsBuilderClass(),
                    returnType = UNIT,
                ).copy(suspending = true),
            )
            .addStatement("val builder = %T()", customArgsBuilderClass())
            .addStatement("block(builder)")
            .addStatement("this.opts = builder.build()")
            .addDocs("@param block A bag of options that control this resource's behavior.")
            .build()

        val resourceBuilderClass = TypeSpec
            .classBuilder(resourceBuilderClassName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addModifiers(INTERNAL)
                    .build(),
            )
            .addAnnotation(pulumiDslMarkerAnnotation())
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
                    PropertySpec.builder("opts", customArgsClass())
                        .mutable(true)
                        .initializer("%T()", customArgsClass())
                        .build(),
                ),
            )
            .addFunction(
                FunSpec.builder("name")
                    .addParameter("value", STRING)
                    .addCode("this.name = value")
                    .addDocs("@param name The _unique_ name of the resulting resource.")
                    .build(),
            )
            .addFunction(argsFunction)
            .addFunction(optsFunction)
            .addFunction(
                FunSpec.builder("build")
                    .addModifiers(INTERNAL)
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
            .addDocs("Builder for [${resourceClassName.simpleName}].")
            .addDeprecationWarningIfAvailable(resourceType.kDoc)
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
            .addDocs(
                """@see [${resourceType.name.name}].
                  |@param name The _unique_ name of the resulting resource.
                  |@param block Builder for [${resourceClassName.simpleName}]."""
                    .trimMargin(),
            )
            .addDeprecationWarningIfAvailable(resourceType.kDoc)
            .build()

        fileSpecBuilder
            .addType(resourceBuilderClass)
            .addType(resourceClass)
            .addImport("com.pulumi.kotlin", "toKotlin")
            .addFunction(resourceFunction)
    }
}

package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Java.Pulumi.outputOfMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.applySuspendExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.applyValueExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.convertibleToJavaClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.pulumiDslMarkerAnnotation
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.toJavaExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.toKotlinExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NameGeneration
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.addImports
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.addTypes
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDeprecationWarningIfAvailable
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDocs
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToJava.toJavaFunction
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToKotlin.toKotlinFunction
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters.AllSetterGenerators
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters.Setter
import com.virtuslab.pulumikotlin.codegen.utils.letIf
import java.util.Random
import kotlin.streams.asSequence

object TypeGenerator {

    fun generateTypes(
        types: List<RootType>,
        generationOptions: GenerationOptions = GenerationOptions(),
        typeNameClashResolver: TypeNameClashResolver,
    ): List<FileSpec> {
        val generatedTypes = types.filterIsInstance<ComplexType>().map { type ->
            val usageKind = type.metadata.usageKind
            val isFunction = usageKind.subject == Function
            val isOutput = usageKind.direction == Output
            val fields = if (isFunction || isOutput) {
                prepareFieldsForTypesUsedForFunctionsOrAsOutput(type)
            } else {
                prepareFields(type)
            }

            generateFile(Context(type.metadata, fields, generationOptions), typeNameClashResolver)
        }

        return generatedTypes.plus(EnumTypeGenerator.generateEnums(types, typeNameClashResolver))
    }

    private fun prepareFields(type: ComplexType) =
        type.fields.map { (name, typeAndOptionality) ->
            Field(
                name,
                OutputWrappedField(typeAndOptionality.type),
                typeAndOptionality.required,
                listOf(
                    NormalField(typeAndOptionality.type) { argument ->
                        outputOfMethod().invoke(argument)
                    },
                ),
                typeAndOptionality.kDoc,
            )
        }

    private fun prepareFieldsForTypesUsedForFunctionsOrAsOutput(type: ComplexType) =
        type.fields.map { (name, typeAndOptionality) ->
            Field(
                name,
                NormalField(typeAndOptionality.type) { it },
                typeAndOptionality.required,
                overloads = emptyList(),
                typeAndOptionality.kDoc,
            )
        }

    private fun generateFile(context: Context, typeNameClashResolver: TypeNameClashResolver): FileSpec {
        val typeMetadata = context.typeMetadata
        val useAlternativeName = typeNameClashResolver.shouldUseAlternativeName(typeMetadata)
        val names = typeNameClashResolver.kotlinNames(typeMetadata)

        return FileSpec
            .builder(names.packageName, names.className)
            .addImports(
                applySuspendExtensionMethod(),
                applyValueExtensionMethod(),
                toJavaExtensionMethod(),
                toKotlinExtensionMethod(),
            )
            .addTypes(
                generateArgsClass(context, useAlternativeName, names, typeNameClashResolver),
                generateArgsBuilderClass(context, names, typeNameClashResolver),
            )
            .build()
    }

    private fun generateArgsClass(
        context: Context,
        useAlternativeName: Boolean,
        kotlinNames: NameGeneration,
        typeNameClashResolver: TypeNameClashResolver,
    ): TypeSpec {
        val (typeMetadata, fields, options) = context

        val (constructor, properties) = generatePropertiesPerFieldWithConstructor(context, typeNameClashResolver)

        val classDocs = typeMetadata.kDoc.description.orEmpty()
        val propertyDocs = fields.joinToString("\n") {
            "@property ${it.name} ${it.kDoc.description.orEmpty()}"
        }

        return TypeSpec.classBuilder(argsClassName(kotlinNames))
            .addModifiers(KModifier.DATA)
            .primaryConstructor(constructor)
            .addProperties(properties)
            .addDocs("$classDocs\n$propertyDocs")
            .letIf(options.implementToJava) {
                val javaNames = javaNames(context, useAlternativeName)
                val innerType = javaNames.kotlinPoetClassName
                val convertibleToJava = convertibleToJavaClass().parameterizedBy(innerType)
                it
                    .addSuperinterface(convertibleToJava)
                    .addFunction(toJavaFunction(fields, javaNames))
            }
            .letIf(options.implementToKotlin) {
                it.addType(
                    TypeSpec.companionObjectBuilder()
                        .addFunction(
                            toKotlinFunction(
                                typeMetadata,
                                useAlternativeName,
                                kotlinNames,
                                fields,
                                typeNameClashResolver,
                            ),
                        )
                        .build(),
                )
            }
            .build()
    }

    private fun generateArgsBuilderClass(
        context: Context,
        names: NameGeneration,
        typeNameClashResolver: TypeNameClashResolver,
    ): TypeSpec {
        return TypeSpec
            .classBuilder(argsBuilderClassName(names))
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(INTERNAL)
                    .build(),
            )
            .addAnnotation(pulumiDslMarkerAnnotation())
            .addProperties(generatePropertiesPerField(context, typeNameClashResolver))
            .addFunctions(
                generateMethodsPerField(context, typeNameClashResolver) + generateBuildMethod(context, names),
            )
            .addDeprecationWarningIfAvailable(context.typeMetadata.kDoc)
            .addDocs("Builder for [${argsClassName(names).simpleName}].")
            .build()
    }

    private fun generatePropertiesPerField(
        context: Context,
        typeNameClashResolver: TypeNameClashResolver,
    ): List<PropertySpec> {
        return context.fields.map {
            PropertySpec
                .builder(it.name, it.toNullableTypeName(typeNameClashResolver))
                .initializer("null")
                .mutable(true)
                .addModifiers(KModifier.PRIVATE)
                .build()
        }
    }

    private fun generatePropertiesPerFieldWithConstructor(
        context: Context,
        typeNameClashResolver: TypeNameClashResolver,
    ): Pair<FunSpec, List<PropertySpec>> {
        val properties = context.fields.map { field ->
            PropertySpec
                .builder(field.name, field.toTypeName(typeNameClashResolver))
                .initializer(field.name)
                .addDeprecationWarningIfAvailable(field.kDoc)
                .build()
        }

        val constructorParameters = context.fields.map { field ->
            ParameterSpec.builder(field.name, field.toTypeName(typeNameClashResolver))
                .letIf(!field.required) {
                    it.defaultValue("%L", null)
                }
                .build()
        }
        val constructor = FunSpec
            .constructorBuilder()
            .addDeprecationWarningIfAvailable(context.typeMetadata.kDoc)
            .addParameters(constructorParameters)
            .build()

        return Pair(constructor, properties)
    }

    /**
     * Example output:
     *
     * ```
     * fun build(): BucketArgs {
     *   BucketArgs(
     *      name1 = name1!!
     *      name2 = name2
     *      name3 = name3
     *   )
     * }
     * ```
     */
    private fun generateBuildMethod(context: Context, names: NameGeneration): FunSpec {
        val arguments = context.fields.associate {
            val requiredPart = if (it.required) "!!" else ""
            it.name to CustomExpressionBuilder.start("%N$requiredPart", it.name).build()
        }

        return FunSpec.builder("build")
            .addModifiers(INTERNAL)
            .returns(argsClassName(names))
            .addCode(Return(ConstructObjectExpression(argsClassName(names), fields = arguments)))
            .build()
    }

    private fun generateMethodsPerField(context: Context, typeNameClashResolver: TypeNameClashResolver): List<FunSpec> {
        val fields = context.fields

        val normalFunctions = fields.map { field -> Setter.from(field) }
        val overloadFunctions = fields.flatMap { field ->
            field.overloads.map { overload ->
                Setter.from(field, overload)
            }
        }
        val allRecipes = normalFunctions + overloadFunctions

        return allRecipes
            .flatMap { generateMethodsForField(it, typeNameClashResolver) }
            .map { withoutJvmPlatformNameClashRisk(it) }
    }

    private fun generateMethodsForField(
        field: Setter,
        typeNameClashResolver: TypeNameClashResolver,
    ): Iterable<FunSpec> {
        return AllSetterGenerators.generate(field, typeNameClashResolver)
    }

    private fun argsBuilderClassName(names: NameGeneration): ClassName {
        return ClassName(names.packageName, names.builderClassName)
    }

    private fun argsClassName(names: NameGeneration): ClassName {
        return ClassName(names.packageName, names.className)
    }

    private fun kotlinNames(context: Context, useAlternativeName: Boolean) =
        context.typeMetadata.names(Kotlin, useAlternativeName)

    private fun javaNames(context: Context, useAlternativeName: Boolean) =
        context.typeMetadata.names(Java, useAlternativeName)

    private fun withoutJvmPlatformNameClashRisk(funSpec: FunSpec): FunSpec {
        val randomJvmNameAnnotation = AnnotationSpec
            .builder(JvmName::class)
            .addMember("%S", randomStringWith16Characters())
            .build()

        return funSpec
            .toBuilder()
            .addAnnotation(randomJvmNameAnnotation)
            .build()
    }

    // The number of characters does not matter here, 16 is an arbitrary number.
    @Suppress("MagicNumber")
    private fun randomStringWith16Characters() =
        Random()
            .ints('a'.code, 'z'.code)
            .asSequence()
            .map { it.toChar() }
            .take(16)
            .joinToString("")

    data class GenerationOptions(
        val shouldGenerateBuilders: Boolean = true,
        val implementToJava: Boolean = true,
        val implementToKotlin: Boolean = true,
    )

    private data class Context(
        val typeMetadata: TypeMetadata,
        val fields: List<Field<ReferencedType>>,
        val options: GenerationOptions,
    )
}

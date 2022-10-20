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
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Java.Pulumi.outputOfMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.applySuspendExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.applyValueExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.convertibleToJavaClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.pulumiDslMarkerAnnotation
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.toJavaExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.toKotlinExtensionMethod
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.addImports
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.addTypes
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField
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

            generateFile(Context(type.metadata, fields, generationOptions))
        }

        return generatedTypes.plus(EnumTypeGenerator.generateEnums(types))
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

    private fun generateFile(context: Context): FileSpec {
        val names = kotlinNames(context)

        return FileSpec
            .builder(names.packageName, names.className)
            .addImports(
                applySuspendExtensionMethod(),
                applyValueExtensionMethod(),
                toJavaExtensionMethod(),
                toKotlinExtensionMethod(),
            )
            .addTypes(
                generateArgsClass(context),
                generateArgsBuilderClass(context),
            )
            .build()
    }

    private fun generateArgsClass(context: Context): TypeSpec {
        val (typeMetadata, fields, options) = context

        val (constructor, properties) = generatePropertiesPerFieldWithConstructor(context)

        val classDocs = typeMetadata.kDoc.description.orEmpty()
        val propertyDocs = fields.joinToString("\n") {
            "@property ${it.name} ${it.kDoc.description.orEmpty()}"
        }

        return TypeSpec.classBuilder(argsClassName(context))
            .addModifiers(KModifier.DATA)
            .primaryConstructor(constructor)
            .addProperties(properties)
            .addDocs("$classDocs\n$propertyDocs")
            .letIf(options.implementToJava) {
                val innerType = typeMetadata.names(LanguageType.Java).kotlinPoetClassName
                val convertibleToJava = convertibleToJavaClass().parameterizedBy(innerType)
                it
                    .addSuperinterface(convertibleToJava)
                    .addFunction(toJavaFunction(typeMetadata, fields))
            }
            .letIf(options.implementToKotlin) {
                it.addType(
                    TypeSpec.companionObjectBuilder()
                        .addFunction(toKotlinFunction(typeMetadata, fields))
                        .build(),
                )
            }
            .build()
    }

    private fun generateArgsBuilderClass(context: Context): TypeSpec {
        return TypeSpec
            .classBuilder(argsBuilderClassName(context))
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(INTERNAL)
                    .build(),
            )
            .addAnnotation(pulumiDslMarkerAnnotation())
            .addProperties(generatePropertiesPerField(context))
            .addFunctions(generateMethodsPerField(context) + generateBuildMethod(context))
            .addDeprecationWarningIfAvailable(context.typeMetadata.kDoc)
            .addDocs("Builder for [${argsClassName(context).simpleName}].")
            .build()
    }

    private fun generatePropertiesPerField(context: Context): List<PropertySpec> {
        return context.fields.map {
            PropertySpec
                .builder(it.name, it.toNullableTypeName())
                .initializer("null")
                .mutable(true)
                .addModifiers(KModifier.PRIVATE)
                .build()
        }
    }

    private fun generatePropertiesPerFieldWithConstructor(context: Context): Pair<FunSpec, List<PropertySpec>> {
        val properties = context.fields.map { field ->
            PropertySpec
                .builder(field.name, field.toTypeName())
                .initializer(field.name)
                .addDeprecationWarningIfAvailable(field.kDoc)
                .build()
        }

        val constructorParameters = context.fields.map { field ->
            ParameterSpec.builder(field.name, field.toTypeName())
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
    private fun generateBuildMethod(context: Context): FunSpec {
        val arguments = context.fields.associate {
            val requiredPart = if (it.required) "!!" else ""
            it.name to CustomExpressionBuilder.start("%N$requiredPart", it.name).build()
        }

        return FunSpec.builder("build")
            .addModifiers(INTERNAL)
            .returns(argsClassName(context))
            .addCode(Return(ConstructObjectExpression(argsClassName(context), fields = arguments)))
            .build()
    }

    private fun generateMethodsPerField(context: Context): List<FunSpec> {
        val fields = context.fields

        val normalFunctions = fields.map { field -> Setter.from(field) }
        val overloadFunctions = fields.flatMap { field ->
            field.overloads.map { overload ->
                Setter.from(field, overload)
            }
        }
        val allRecipes = normalFunctions + overloadFunctions

        return allRecipes
            .flatMap { generateMethodsForField(it) }
            .map { withoutJvmPlatformNameClashRisk(it) }
    }

    private fun generateMethodsForField(field: Setter): Iterable<FunSpec> {
        return AllSetterGenerators.generate(field)
    }

    private fun argsBuilderClassName(context: Context): ClassName {
        val names = kotlinNames(context)
        return ClassName(names.packageName, names.builderClassName)
    }

    private fun argsClassName(context: Context): ClassName {
        val names = kotlinNames(context)
        return ClassName(names.packageName, names.className)
    }

    private fun kotlinNames(context: Context) = context.typeMetadata.names(Kotlin)

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

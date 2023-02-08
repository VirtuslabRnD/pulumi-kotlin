package org.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.pulumi.kotlin.PulumiNullFieldException
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import org.virtuslab.pulumikotlin.codegen.expressions.Return
import org.virtuslab.pulumikotlin.codegen.expressions.addCode
import org.virtuslab.pulumikotlin.codegen.expressions.invoke
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Java.Pulumi.outputOfMethod
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.applySuspendExtensionMethod
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.convertibleToJavaClass
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.pulumiDslMarkerAnnotation
import org.virtuslab.pulumikotlin.codegen.step2intermediate.NameGeneration
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import org.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import org.virtuslab.pulumikotlin.codegen.step3codegen.Field
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.addImports
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.addTypes
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addStandardSuppressAnnotations
import org.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import org.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField
import org.virtuslab.pulumikotlin.codegen.step3codegen.ToKotlin
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDeprecationWarningIfAvailable
import org.virtuslab.pulumikotlin.codegen.step3codegen.addDocs
import org.virtuslab.pulumikotlin.codegen.step3codegen.types.setters.AllSetterGenerators
import org.virtuslab.pulumikotlin.codegen.step3codegen.types.setters.Setter
import org.virtuslab.pulumikotlin.codegen.utils.letIf
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
                typeAndOptionality.type !is OptionalType,
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
                typeAndOptionality.type !is OptionalType,
                overloads = emptyList(),
                typeAndOptionality.kDoc,
            )
        }

    private fun generateFile(context: Context, typeNameClashResolver: TypeNameClashResolver): FileSpec {
        val typeMetadata = context.typeMetadata
        val names = typeNameClashResolver.kotlinNames(typeMetadata)

        return FileSpec
            .builder(names.packageName, names.className)
            .addStandardSuppressAnnotations()
            .addImports(
                applySuspendExtensionMethod(),
            )
            .addTypes(
                generateArgsClass(context, names, typeNameClashResolver),
            )
            .letIf(names.shouldConstructBuilders) {
                it.addType(generateArgsBuilderClass(context, names, typeNameClashResolver))
            }
            .build()
    }

    private fun generateArgsClass(
        context: Context,
        kotlinNames: NameGeneration,
        typeNameClashResolver: TypeNameClashResolver,
    ): TypeSpec {
        val (typeMetadata, fields, options) = context

        val (constructor, properties) = generatePropertiesPerFieldWithConstructor(context, typeNameClashResolver)

        val classDocs = typeMetadata.kDoc.description.orEmpty()
        val propertyDocs = fields.joinToString("\n") {
            "@property ${it.toKotlinName()} ${it.kDoc.description.orEmpty()}"
        }

        val kotlinClass = kotlinNames.kotlinPoetClassName
        val javaClass = typeNameClashResolver.javaNames(context.typeMetadata).kotlinPoetClassName

        return TypeSpec.classBuilder(kotlinClass)
            .letIf(fields.isNotEmpty()) {
                it.addModifiers(KModifier.DATA)
            }
            .primaryConstructor(constructor)
            .addProperties(properties)
            .addDocs("$classDocs\n$propertyDocs")
            .letIf(options.implementToJava && kotlinNames.shouldImplementToJava) {
                it
                    .addSuperinterface(convertibleToJavaClass().parameterizedBy(javaClass))
                    .addFunction(ToJava.typeFunction(fields, typeNameClashResolver, context.typeMetadata))
            }
            .letIf(options.implementToKotlin && kotlinNames.shouldImplementToKotlin) {
                it.addType(
                    TypeSpec.companionObjectBuilder()
                        .addFunction(
                            ToKotlin.typeFunction(
                                typeMetadata,
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
        kotlinNames: NameGeneration,
        typeNameClashResolver: TypeNameClashResolver,
    ): TypeSpec {
        return TypeSpec.classBuilder(kotlinNames.builderClassName)
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(INTERNAL)
                    .build(),
            )
            .addAnnotation(pulumiDslMarkerAnnotation())
            .addProperties(generatePropertiesPerField(context, typeNameClashResolver))
            .addFunctions(
                generateMethodsPerField(context, typeNameClashResolver) + generateBuildMethod(context, kotlinNames),
            )
            .addDeprecationWarningIfAvailable(context.typeMetadata.kDoc)
            .addDocs("Builder for [${kotlinNames.kotlinPoetClassName.simpleName}].")
            .build()
    }

    private fun generatePropertiesPerField(
        context: Context,
        typeNameClashResolver: TypeNameClashResolver,
    ): List<PropertySpec> {
        return context.fields.map {
            PropertySpec
                .builder(it.toKotlinName(), it.toNullableTypeName(typeNameClashResolver))
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
                .builder(field.toKotlinName(), field.toTypeName(typeNameClashResolver))
                .initializer(field.toKotlinName())
                .addDeprecationWarningIfAvailable(field.kDoc)
                .build()
        }

        val constructorParameters = context.fields.map { field ->
            ParameterSpec.builder(field.toKotlinName(), field.toTypeName(typeNameClashResolver))
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
        val arguments = context.fields.associate { type ->
            type.toKotlinName() to CustomExpressionBuilder.start("%N", type.toKotlinName())
                .letIf(type.required) {
                    it.plus(
                        CustomExpression(" ?: throw %T(%S)", PulumiNullFieldException::class, type.toKotlinName()),
                    )
                }
                .build()
        }

        return FunSpec.builder("build")
            .addModifiers(INTERNAL)
            .returns(names.kotlinPoetClassName)
            .addCode(Return(ConstructObjectExpression(names.kotlinPoetClassName, fields = arguments)))
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
        val implementToJava: Boolean = true,
        val implementToKotlin: Boolean = true,
    )

    private data class Context(
        val typeMetadata: TypeMetadata,
        val fields: List<Field<ReferencedType>>,
        val options: GenerationOptions,
    )
}

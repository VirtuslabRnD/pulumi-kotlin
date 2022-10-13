package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.virtuslab.pulumikotlin.codegen.expressions.Assignment
import com.virtuslab.pulumikotlin.codegen.expressions.ConstructObjectExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import com.virtuslab.pulumikotlin.codegen.expressions.CustomExpressionBuilder
import com.virtuslab.pulumikotlin.codegen.expressions.Return
import com.virtuslab.pulumikotlin.codegen.expressions.add
import com.virtuslab.pulumikotlin.codegen.expressions.addCode
import com.virtuslab.pulumikotlin.codegen.expressions.callLet
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.ConvertibleToJava
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.FieldType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderLambda
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.listOfLambdas
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import com.virtuslab.pulumikotlin.codegen.step3codegen.OutputWrappedField
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDeprecationWarningIfAvailable
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDocs
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToJava.toJavaFunction
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToKotlin.toKotlinFunction
import com.virtuslab.pulumikotlin.codegen.utils.letIf
import java.util.Random
import kotlin.streams.asSequence

object TypeGenerator {
    data class GenerationOptions(
        val shouldGenerateBuilders: Boolean = true,
        val implementToJava: Boolean = true,
        val implementToKotlin: Boolean = true,
    )

    fun generateTypes(
        types: List<RootType>,
        generationOptions: GenerationOptions = GenerationOptions(),
    ): List<FileSpec> {
        val generatedTypes = types.filterIsInstance<ComplexType>().map { type ->
            val usageKind = type.metadata.usageKind
            val isFunction = usageKind.subject == Function
            val isOutput = usageKind.direction == Output
            val fields = if (isFunction || isOutput) {
                type.fields.map { (name, typeAndOptionality) ->
                    Field(
                        name,
                        NormalField(typeAndOptionality.type) { expr -> expr },
                        typeAndOptionality.required,
                        overloads = emptyList(),
                        typeAndOptionality.kDoc,
                    )
                }
            } else {
                type.fields.map { (name, typeAndOptionality) ->
                    Field(
                        name,
                        OutputWrappedField(typeAndOptionality.type),
                        typeAndOptionality.required,
                        listOf(
                            NormalField(typeAndOptionality.type) { argument ->
                                MoreTypes.Java.Pulumi.Output.of(argument)
                            },
                        ),
                        typeAndOptionality.kDoc,
                    )
                }
            }

            generateType(type.metadata, fields, generationOptions)
        }

        return generatedTypes.plus(EnumTypeGenerator.generateEnums(types))
    }

    private fun generateType(
        typeMetadata: TypeMetadata,
        fields: List<Field<*>>,
        options: GenerationOptions = GenerationOptions(),
    ): FileSpec {
        val names = typeMetadata.names(LanguageType.Kotlin)

        val fileSpec = FileSpec.builder(
            names.packageName,
            names.className,
        )

        val argsClassName = ClassName(names.packageName, names.className)

        val dslTag = ClassName("com.pulumi.kotlin", "PulumiTagMarker")

        val classBuilder = TypeSpec.classBuilder(argsClassName)
            .letIf(options.implementToJava) {
                val convertibleToJava = ConvertibleToJava(typeMetadata.names(LanguageType.Java).kotlinPoetClassName)
                it.addSuperinterface(convertibleToJava)
            }
            .addModifiers(KModifier.DATA)

        val constructor = FunSpec.constructorBuilder()
            .addDeprecationWarningIfAvailable(typeMetadata.kDoc)

        fields.forEach { field ->
            val isRequired = field.required
            val typeName = field.fieldType.toTypeName().copy(nullable = !isRequired)

            classBuilder.addProperty(
                PropertySpec.builder(field.name, typeName)
                    .initializer(field.name)
                    .addDeprecationWarningIfAvailable(field.kDoc)
                    .build(),
            )

            constructor.addParameter(
                ParameterSpec.builder(field.name, typeName)
                    .letIf(!isRequired) {
                        it.defaultValue("%L", null)
                    }
                    .build(),
            )
        }

        classBuilder.primaryConstructor(constructor.build())

        classBuilder.letIf(options.implementToJava) {
            it.addFunction(toJavaFunction(typeMetadata, fields))
        }
        classBuilder.letIf(options.implementToKotlin) {
            it.addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(toKotlinFunction(typeMetadata, fields))
                    .build(),
            )
        }

        val classDocs = typeMetadata.kDoc.description.orEmpty()
        val propertyDocs = fields.joinToString("\n") {
            "@property ${it.name} ${it.kDoc.description.orEmpty()}"
        }

        classBuilder.addDocs("$classDocs\n$propertyDocs")

        val argsClass = classBuilder.build()

        val argsBuilderClassName = ClassName(names.packageName, names.builderClassName)

        val arguments = fields.associate {
            val requiredPart = if (it.required) "!!" else ""
            it.name to CustomExpressionBuilder.start("%N$requiredPart", it.name).build()
        }

        val argsBuilderClass = TypeSpec
            .classBuilder(argsBuilderClassName)
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(INTERNAL)
                    .build(),
            )
            .addAnnotation(dslTag)
            .addProperties(
                fields.map {
                    PropertySpec
                        .builder(it.name, it.fieldType.toTypeName().copy(nullable = true))
                        .initializer("null")
                        .mutable(true)
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                },
            )
            .addFunctions(
                fields.flatMap { field ->
                    generateFunctionsForInput(field)
                },
            )
            .addFunction(
                FunSpec.builder("build")
                    .addModifiers(INTERNAL)
                    .returns(argsClassName)
                    .addCode(Return(ConstructObjectExpression(argsClassName, arguments)))
                    .build(),
            )
            .addDeprecationWarningIfAvailable(typeMetadata.kDoc)
            .addDocs("Builder for [${argsClassName.simpleName}].")
            .build()

        fileSpec
            .addImport("com.pulumi.kotlin", "applySuspend")
            .addImport("com.pulumi.kotlin", "applyValue")
            .addImport("com.pulumi.kotlin", "toJava")
            .addImport("com.pulumi.kotlin", "toKotlin")
            .let {
                if (options.shouldGenerateBuilders) {
                    it.addType(argsBuilderClass)
                } else {
                    it
                }
            }
            .addType(argsClass)

        return fileSpec.build()
    }

    private fun mappingCodeBlock(
        field: NormalField<*>,
        required: Boolean,
        name: String,
        code: String,
        vararg args: Any?,
    ): CodeBlock {
        val expression = CustomExpression("toBeMapped").callLet(!required) { arg -> field.mappingCode(arg) }
        return CodeBlock.builder()
            .addStatement("val toBeMapped = $code", *args)
            .add(Assignment("mapped", expression))
            .addStatement("")
            .addStatement("this.%N = mapped", name)
            .build()
    }

    private fun builderPattern(
        name: String,
        parameterType: TypeName,
        kDoc: KDoc,
        codeBlock: KotlinPoetPatterns.BuilderSettingCodeBlock,
        parameterModifiers: List<KModifier> = emptyList(),
    ): FunSpec {
        return FunSpec
            .builder(name)
            .addModifiers(SUSPEND)
            .addParameter(
                "argument",
                parameterType,
                parameterModifiers,
            )
            .addCode(codeBlock.toCodeBlock(name))
            .addDocsToBuilderMethod(kDoc, "argument")
            .build()
    }

    private fun FunSpec.Builder.preventJvmPlatformNameClash(): FunSpec.Builder {
        return addAnnotation(
            AnnotationSpec.builder(JvmName::class)
                .addMember("%S", randomStringWith16Characters())
                .build(),
        )
    }

    private fun randomStringWith16Characters() =
        Random().ints('a'.code, 'z'.code).asSequence().map { it.toChar() }.take(16).joinToString("")

    private fun specialMethodsForComplexType(
        name: String,
        field: NormalField<ReferencedComplexType>,
        kDoc: KDoc,
    ): List<FunSpec> {
        val builderTypeName = field.type.toBuilderTypeName()
        return listOf(
            builderPattern(
                name,
                builderLambda(builderTypeName),
                kDoc,
                KotlinPoetPatterns.BuilderSettingCodeBlock
                    .create("%T().applySuspend{ argument() }.build()", builderTypeName)
                    .withMappingCode(field.mappingCode),
            ),
        )
    }

    private fun specialMethodsForList(
        name: String,
        field: NormalField<ListType>,
        kDoc: KDoc,
    ): List<FunSpec> {
        val innerType = field.type.innerType
        val builderPattern = when (innerType) {
            is ReferencedComplexType -> {
                val commonCodeBlock = KotlinPoetPatterns.BuilderSettingCodeBlock
                    .create(
                        "argument.toList().map { %T().applySuspend{ it() }.build() }",
                        innerType.toBuilderTypeName(),
                    )
                    .withMappingCode(field.mappingCode)

                listOf(
                    builderPattern(name, listOfLambdas(innerType), kDoc, commonCodeBlock),
                    builderPattern(
                        name,
                        builderLambda(innerType),
                        kDoc,
                        commonCodeBlock,
                        parameterModifiers = listOf(VARARG),
                    ),
                )
            }

            else -> emptyList()
        }

        val justValuesPassedAsVarargArguments = listOf(
            FunSpec
                .builder(name)
                .addModifiers(SUSPEND)
                .addParameter("values", innerType.toTypeName(), VARARG)
                .addCode(mappingCodeBlock(field, false, name, "values.toList()"))
                .addDocsToBuilderMethod(kDoc, "values")
                .build(),
        )

        return builderPattern + justValuesPassedAsVarargArguments
    }

    private fun specialMethodsForMap(
        name: String,
        field: NormalField<MapType>,
        kDoc: KDoc,
    ): List<FunSpec> {
        val leftInnerType = field.type.firstType
        val rightInnerType = field.type.secondType

        val builderPattern = when (rightInnerType) {
            is ReferencedComplexType -> {
                val commonCodeBlock = KotlinPoetPatterns.BuilderSettingCodeBlock
                    .create(
                        "argument.toList().map { (left, right) -> left to %T().applySuspend{ right() }.build() }",
                        rightInnerType.toBuilderTypeName(),
                    )
                    .withMappingCode(field.mappingCode)

                listOf(
                    builderPattern(
                        name,
                        MoreTypes.Kotlin.Pair(leftInnerType.toTypeName(), builderLambda(rightInnerType)),
                        kDoc,
                        commonCodeBlock,
                        parameterModifiers = listOf(VARARG),
                    ),
                )
            }

            else -> emptyList()
        }

        val justValuesPassedAsVarargArguments = listOf(
            FunSpec
                .builder(name)
                .addParameter(
                    "values",
                    MoreTypes.Kotlin.Pair(leftInnerType.toTypeName(), rightInnerType.toTypeName()),
                    VARARG,
                )
                .addCode(mappingCodeBlock(field, false, name, "values.toMap()"))
                .addDocsToBuilderMethod(kDoc, "values")
                .build(),
        )

        return builderPattern + justValuesPassedAsVarargArguments
    }

    private fun generateFunctionsForInput(field: Field<*>): List<FunSpec> {
        val functionsForDefaultField = generateFunctionsForInput2(
            field.name,
            field.required,
            field.fieldType,
            field.kDoc,
        )

        val functionsForOverloads = field.overloads.flatMap {
            generateFunctionsForInput2(field.name, field.required, it, field.kDoc)
        }

        val allFunctions = functionsForDefaultField + functionsForOverloads

        return allFunctions.map {
            it
                .toBuilder()
                .preventJvmPlatformNameClash()
                .build()
        }
    }

    private fun generateFunctionsForInput2(
        name: String,
        required: Boolean,
        fieldType: FieldType<*>,
        kDoc: KDoc,
    ): List<FunSpec> {
        val functions = when (fieldType) {
            is NormalField -> {
                val basicFunction = FunSpec
                    .builder(name)
                    .addModifiers(SUSPEND)
                    .addParameter("value", fieldType.toTypeName().copy(nullable = !required))
                    .addCode(mappingCodeBlock(fieldType, required, name, "value"))
                    .addDocsToBuilderMethod(kDoc, "value")
                    .build()

                @Suppress("UNCHECKED_CAST")
                val otherFunctions = when (fieldType.type) {
                    is ReferencedComplexType -> specialMethodsForComplexType(
                        name,
                        fieldType as NormalField<ReferencedComplexType>,
                        kDoc,
                    )

                    is ListType -> specialMethodsForList(name, fieldType as NormalField<ListType>, kDoc)
                    is MapType -> specialMethodsForMap(name, fieldType as NormalField<MapType>, kDoc)
                    is PrimitiveType -> listOf()
                    is EitherType -> listOf()
                    else -> listOf()
                }

                otherFunctions + basicFunction
            }

            is OutputWrappedField -> {
                listOf(
                    FunSpec
                        .builder(name)
                        .addModifiers(SUSPEND)
                        .addParameter("value", fieldType.toTypeName().copy(nullable = !required))
                        .addCode("this.%N = value", name)
                        .addDocsToBuilderMethod(kDoc, "value")
                        .build(),
                )
            }
        }

        return functions
    }

    private fun FunSpec.Builder.addDocsToBuilderMethod(kDoc: KDoc, paramName: String) = apply {
        addDocs("@param $paramName ${kDoc.description.orEmpty()}")
        addDeprecationWarningIfAvailable(kDoc)
    }
}

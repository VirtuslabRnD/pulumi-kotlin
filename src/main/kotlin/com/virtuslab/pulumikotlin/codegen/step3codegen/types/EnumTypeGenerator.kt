package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EnumValue
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.convertibleToJavaClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import com.virtuslab.pulumikotlin.codegen.step3codegen.addDocsIfAvailable
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToJava.toJavaEnumFunction
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToKotlin.toKotlinEnumFunction

private const val PROPERTY_NAME = "javaValue"

object EnumTypeGenerator {

    fun generateEnums(
        types: List<RootType>,
        typeNameClashResolver: TypeNameClashResolver,
    ): List<FileSpec> {
        return types.filterIsInstance<EnumType>()
            .map {
                val enumTypeSpec = buildEnumTypeSpec(it, typeNameClashResolver)
                buildEnumFileSpec(it, enumTypeSpec, typeNameClashResolver)
            }
    }

    private fun buildEnumTypeSpec(enumType: EnumType, typeNameClashResolver: TypeNameClashResolver): TypeSpec {
        val javaEnumClassName = enumType.metadata.names(LanguageType.Java).kotlinPoetClassName

        return TypeSpec.enumBuilder(enumType.metadata.pulumiName.name)
            .addSuperinterface(prepareConvertibleToJavaInterface(enumType.metadata, typeNameClashResolver))
            .addFunction(toJavaEnumFunction(enumType.metadata))
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(toKotlinEnumFunction(enumType.metadata, typeNameClashResolver))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder(PROPERTY_NAME, javaEnumClassName)
                    .initializer(PROPERTY_NAME)
                    .build(),
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(PROPERTY_NAME, javaEnumClassName)
                    .build(),
            )
            .addDocsIfAvailable(enumType.metadata.kDoc)
            .addEnumConstants(enumType.possibleValues, javaEnumClassName)
            .build()
    }

    private fun prepareConvertibleToJavaInterface(
        typeMetadata: TypeMetadata,
        typeNameClashResolver: TypeNameClashResolver,
    ): ParameterizedTypeName {
        val javaNames = typeNameClashResolver.javaNames(typeMetadata)
        val javaClass = ClassName(javaNames.packageName, javaNames.className)
        return convertibleToJavaClass().parameterizedBy(javaClass)
    }

    private fun buildEnumFileSpec(
        enumType: EnumType,
        enumTypeSpec: TypeSpec,
        typeNameClashResolver: TypeNameClashResolver,
    ): FileSpec {
        val kotlinNames = typeNameClashResolver.kotlinNames(enumType.metadata)
        return FileSpec.builder(
            kotlinNames.packageName,
            kotlinNames.className,
        )
            .addType(enumTypeSpec)
            .build()
    }

    private fun TypeSpec.Builder.addEnumConstants(
        possibleValues: List<EnumValue>,
        javaEnumClassName: ClassName,
    ): TypeSpec.Builder {
        possibleValues.forEach {
            addEnumConstant(it, javaEnumClassName)
        }
        return this
    }

    private fun TypeSpec.Builder.addEnumConstant(enumValue: EnumValue, javaEnumClassName: ClassName): TypeSpec.Builder {
        val escapedEnumValue = try {
            EnumValueGenerator.makeSafeEnumName(enumValue.value)
        } catch (e: IllegalArgumentException) {
            throw InvalidEnumName(javaEnumClassName, e)
        }
        return addEnumConstant(
            escapedEnumValue,
            TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%T.%L", javaEnumClassName, escapedEnumValue)
                .addDocsIfAvailable(enumValue.kDoc)
                .build(),
        )
    }

    class InvalidEnumName(className: ClassName, cause: Throwable) : IllegalArgumentException(
        "Failed to generate enum name for Java class $className",
        cause,
    )
}

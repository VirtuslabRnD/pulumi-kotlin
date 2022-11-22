package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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

        val enumBuilder = TypeSpec.enumBuilder(enumType.metadata.pulumiName.name)
            .addSuperinterface(prepareConvertibleToJavaInterface(enumType.metadata, typeNameClashResolver))
            .addFunction(toJavaEnumFunction(enumType.metadata))
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(toKotlinEnumFunction(enumType.metadata, typeNameClashResolver))
                    .build(),
            )
            .addProperty(PROPERTY_NAME, javaEnumClassName)
            .primaryConstructor(createPrimaryConstructor(javaEnumClassName))
            .addDocsIfAvailable(enumType.metadata.kDoc)

        enumType.possibleValues.forEach {
            enumBuilder.addEnumConstant(it, javaEnumClassName)
        }

        return enumBuilder.build()
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

    private fun createPrimaryConstructor(javaEnumClassName: ClassName) = FunSpec.constructorBuilder()
        .addParameter(
            ParameterSpec.builder(PROPERTY_NAME, javaEnumClassName)
                .build(),
        )
        .addCode("this.%N·=·%N", PROPERTY_NAME, PROPERTY_NAME)
        .build()

    private fun TypeSpec.Builder.addEnumConstant(enumValue: EnumValue, javaEnumClassName: ClassName): TypeSpec.Builder {
        val escapedEnumValue = try {
            EnumValueGenerator.makeSafeEnumName(enumValue.value)
        } catch (e: IllegalStateException) {
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

    private class InvalidEnumName(className: ClassName, cause: Throwable) : IllegalStateException(
        "Failed to generate enum name for Java class $className",
        cause,
    )
}

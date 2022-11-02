package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.convertibleToJavaClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToJava.toJavaEnumFunction
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToKotlin.toKotlinEnumFunction

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
        val enumBuilder = TypeSpec.enumBuilder(enumType.metadata.pulumiName.name)
            .addSuperinterface(prepareConvertibleToJavaInterface(enumType.metadata, typeNameClashResolver))
            .addFunction(toJavaEnumFunction(enumType.metadata))
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(toKotlinEnumFunction(enumType.metadata, useAlternativeName = false))
                    .build(),
            )

        enumType.possibleValues.forEach { enumBuilder.addEnumConstant(it) }

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
}

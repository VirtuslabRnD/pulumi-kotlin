package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Kotlin.Pulumi.convertibleToJavaClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetExtensions.parameterizedBy
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToJava.toJavaEnumFunction
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.ToKotlin.toKotlinEnumFunction

object EnumTypeGenerator {
    fun generateEnums(
        types: List<RootType>,
    ): List<FileSpec> {
        return types.filterIsInstance<EnumType>()
            .map {
                val enumTypeSpec = buildEnumTypeSpec(it)
                buildEnumFileSpec(it, enumTypeSpec)
            }
    }

    private fun buildEnumTypeSpec(enumType: EnumType): TypeSpec {
        val enumBuilder = TypeSpec.enumBuilder(enumType.metadata.pulumiName.name)
            .addSuperinterface(prepareConvertibleToJavaInterface(enumType.metadata))
            .addFunction(toJavaEnumFunction(enumType.metadata))
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(toKotlinEnumFunction(enumType.metadata))
                    .build(),
            )

        enumType.possibleValues.forEach { enumBuilder.addEnumConstant(it) }

        return enumBuilder.build()
    }

    private fun prepareConvertibleToJavaInterface(typeMetadata: TypeMetadata): ParameterizedTypeName {
        val javaNames = typeMetadata.names(LanguageType.Java)
        val javaClass = ClassName(javaNames.packageName, javaNames.className)
        return convertibleToJavaClass().parameterizedBy(javaClass)
    }

    private fun buildEnumFileSpec(enumType: EnumType, enumTypeSpec: TypeSpec): FileSpec {
        val kotlinNames = enumType.metadata.names(LanguageType.Kotlin)
        return FileSpec.builder(
            kotlinNames.packageName,
            kotlinNames.className,
        )
            .addType(enumTypeSpec)
            .build()
    }
}

package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.pulumi.asset.Archive
import com.pulumi.asset.AssetOrArchive
import com.pulumi.core.Either
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Java
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes.Java.Pulumi.outputClass
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NameGeneration
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedEnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedRootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UsageKind

class TypeNameClashResolver(input: GeneratorArguments) {

    private val syntheticKotlinTypeNames: List<TypeName> = input.types
        .filter { it.metadata.usageKind.depth == Depth.Nested }
        .map { it.toClassName(Kotlin) }

    private fun shouldUseAlternativeName(usageKind: UsageKind, className: ClassName) =
        usageKind.depth == Depth.Root && className in syntheticKotlinTypeNames

    fun shouldUseAlternativeName(typeMetadata: TypeMetadata): Boolean {
        val defaultNames = kotlinNames(typeMetadata, false)
        return shouldUseAlternativeName(
            typeMetadata.usageKind,
            defaultNames.kotlinPoetClassName,
        )
    }

    private fun kotlinNames(typeMetadata: TypeMetadata, useAlternativeName: Boolean): NameGeneration {
        return typeMetadata.names(Kotlin, useAlternativeName)
    }

    fun kotlinNames(typeMetadata: TypeMetadata): NameGeneration {
        val defaultNames = kotlinNames(typeMetadata, false)
        val useAlternativeName = shouldUseAlternativeName(
            typeMetadata.usageKind,
            defaultNames.kotlinPoetClassName,
        )
        return if (useAlternativeName) {
            kotlinNames(typeMetadata, useAlternativeName = true)
        } else {
            defaultNames
        }
    }

    private fun javaNames(typeMetadata: TypeMetadata, useAlternativeName: Boolean): NameGeneration {
        return typeMetadata.names(Java, useAlternativeName)
    }

    fun javaNames(typeMetadata: TypeMetadata): NameGeneration {
        val defaultNames = javaNames(typeMetadata, false)
        val useAlternativeName = shouldUseAlternativeName(
            typeMetadata.usageKind,
            defaultNames.kotlinPoetClassName,
        )
        return if (useAlternativeName) {
            javaNames(typeMetadata, useAlternativeName = true)
        } else {
            defaultNames
        }
    }

    fun toTypeName(type: ReferencedType, languageType: LanguageType): TypeName {
        return when (type) {
            is AnyType -> type.toTypeName()
            is ArchiveType -> type.toTypeName()
            is AssetOrArchiveType -> type.toTypeName()
            is EitherType -> toTypeName(type, languageType)
            is ListType -> toTypeName(type, languageType)
            is MapType -> toTypeName(type, languageType)
            is PrimitiveType -> type.toTypeName(languageType)
            is ReferencedComplexType -> toTypeName(type, languageType)
            is ReferencedEnumType -> toTypeName(type, languageType)
        }
    }

    private fun RootType.toClassName(languageType: LanguageType): ClassName {
        return when (this) {
            is ComplexType -> metadata.names(languageType).kotlinPoetClassName
            is EnumType -> metadata.names(languageType).kotlinPoetClassName
        }
    }

    fun <T : ReferencedType> toTypeName(field: OutputWrappedField<T>, languageType: LanguageType): TypeName {
        return outputClass().parameterizedBy(toTypeName(field.type, languageType))
    }

    fun <T : ReferencedType> toTypeName(field: NormalField<T>, languageType: LanguageType): TypeName {
        return toTypeName(field.type, languageType)
    }

    fun <T : ReferencedType> toTypeName(type: FieldType<T>, languageType: LanguageType): TypeName {
        return when (type) {
            is NormalField -> toTypeName(type, languageType)
            is OutputWrappedField -> toTypeName(type, languageType)
        }
    }

    @Suppress("UnusedReceiverParameter")
    private fun AnyType.toTypeName(): ClassName {
        return ANY
    }

    @Suppress("UnusedReceiverParameter")
    private fun AssetOrArchiveType.toTypeName(): ClassName {
        return AssetOrArchive::class.asTypeName()
    }

    @Suppress("UnusedReceiverParameter")
    private fun ArchiveType.toTypeName(): ClassName {
        return Archive::class.asTypeName()
    }

    private fun toTypeName(type: ListType, languageType: LanguageType): ParameterizedTypeName {
        return LIST.parameterizedBy(toTypeName(type.innerType, languageType))
    }

    private fun toTypeName(type: MapType, languageType: LanguageType): ParameterizedTypeName {
        return MAP.parameterizedBy(toTypeName(type.keyType, languageType), toTypeName(type.valueType, languageType))
    }

    private fun toTypeName(type: EitherType, languageType: LanguageType): ParameterizedTypeName {
        return Either::class.asTypeName()
            .parameterizedBy(
                toTypeName(type.firstType, languageType),
                toTypeName(type.secondType, languageType),
            )
    }

    private fun PrimitiveType.toTypeName(languageType: LanguageType): ClassName {
        require(languageType == Kotlin) { "Types other than $Kotlin not expected" }
        return ClassName("kotlin", name)
    }

    fun toTypeName(type: ReferencedRootType, languageType: LanguageType): ClassName {
        return type.metadata.names(languageType).kotlinPoetClassName
    }
}

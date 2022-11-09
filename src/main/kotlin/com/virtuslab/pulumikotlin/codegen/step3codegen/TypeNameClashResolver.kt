package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.pulumi.asset.Archive
import com.pulumi.asset.AssetOrArchive
import com.pulumi.core.Either
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.JsonType
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

class TypeNameClashResolver(types: List<RootType>) {

    private val syntheticKotlinTypeNames: List<TypeName> = types
        .filter { it.metadata.usageKind.depth == Depth.Nested }
        .map { it.toTypeName(Kotlin) }

    private val syntheticJavaTypeNames: List<TypeName> = types
        .filter { it.metadata.usageKind.depth == Depth.Nested }
        .map { it.toTypeName(Java) }

    private fun shouldUseAlternativeNameInKotlin(usageKind: UsageKind, className: ClassName) =
        usageKind.depth == Depth.Root && className in syntheticKotlinTypeNames

    private fun shouldUseAlternativeNameInJava(usageKind: UsageKind, className: ClassName) =
        usageKind.depth == Depth.Root && className in syntheticJavaTypeNames

    private fun kotlinNames(typeMetadata: TypeMetadata, useAlternativeName: Boolean): NameGeneration {
        return typeMetadata.names(Kotlin, useAlternativeName)
    }

    fun kotlinNames(typeMetadata: TypeMetadata): NameGeneration {
        val defaultNames = kotlinNames(typeMetadata, false)
        val useAlternativeName = shouldUseAlternativeNameInKotlin(
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
        val useAlternativeName = shouldUseAlternativeNameInJava(
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
            is JsonType -> type.toTypeName()
            is EitherType -> toTypeName(type, languageType)
            is ListType -> toTypeName(type, languageType)
            is MapType -> toTypeName(type, languageType)
            is PrimitiveType -> type.toTypeName(languageType)
            is ReferencedComplexType -> toTypeName(type, languageType)
            is ReferencedEnumType -> toTypeName(type, languageType)
        }
    }

    private fun RootType.toTypeName(languageType: LanguageType): TypeName {
        return metadata.names(languageType).kotlinPoetClassName
    }

    fun toTypeName(type: ReferencedRootType, languageType: LanguageType): ClassName {
        return type.metadata.names(languageType).kotlinPoetClassName
    }

    fun <T : ReferencedType> toTypeName(type: FieldType<T>, languageType: LanguageType): TypeName {
        return when (type) {
            is NormalField -> toTypeName(type.type, languageType)
            is OutputWrappedField -> outputClass().parameterizedBy(toTypeName(type.type, languageType))
        }
    }

    @Suppress("UnusedReceiverParameter")
    private fun AnyType.toTypeName(): TypeName {
        return ANY
    }

    @Suppress("UnusedReceiverParameter")
    private fun AssetOrArchiveType.toTypeName(): TypeName {
        return AssetOrArchive::class.asTypeName()
    }

    @Suppress("UnusedReceiverParameter")
    private fun ArchiveType.toTypeName(): TypeName {
        return Archive::class.asTypeName()
    }

    @Suppress("UnusedReceiverParameter")
    private fun JsonType.toTypeName(): TypeName {
        return com.google.gson.JsonElement::class.asTypeName()
    }

    private fun toTypeName(type: ListType, languageType: LanguageType): TypeName {
        return LIST.parameterizedBy(toTypeName(type.innerType, languageType))
    }

    private fun toTypeName(type: MapType, languageType: LanguageType): TypeName {
        return MAP.parameterizedBy(
            toTypeName(type.keyType, languageType),
            toTypeName(type.valueType, languageType),
        )
    }

    private fun toTypeName(type: EitherType, languageType: LanguageType): TypeName {
        return Either::class.asTypeName()
            .parameterizedBy(
                toTypeName(type.firstType, languageType),
                toTypeName(type.secondType, languageType),
            )
    }

    private fun PrimitiveType.toTypeName(languageType: LanguageType): TypeName {
        require(languageType == Kotlin) { "Types other than $Kotlin not expected" }
        return ClassName("kotlin", name)
    }
}

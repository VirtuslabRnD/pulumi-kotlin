package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ClassName
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc

data class TypeMetadata(
    val pulumiName: PulumiName,
    val usageKind: UsageKind,
    val kDoc: KDoc,
    val generatedClass: GeneratedClass = GeneratedClass.NormalClass,
) {

    private fun namingFlags(language: LanguageType, useAlternativeName: Boolean) =
        NamingFlags(
            usageKind.depth,
            usageKind.subject,
            usageKind.direction,
            language,
            generatedClass,
            useAlternativeName,
        )

    fun names(language: LanguageType, useAlternativeName: Boolean = false): NameGeneration {
        return NameGeneration(pulumiName, namingFlags(language, useAlternativeName))
    }
}

data class FieldInfo(val type: ReferencedType, val kDoc: KDoc)

sealed class Type

sealed class RootType : Type() {
    abstract val metadata: TypeMetadata

    abstract fun toReference(): ReferencedRootType
}

sealed class ReferencedRootType : ReferencedType() {
    abstract val metadata: TypeMetadata
}

sealed class ReferencedType : Type()

data class OptionalType(val innerType: ReferencedType) : ReferencedType()

object AnyType : ReferencedType()

data class ReferencedComplexType(override val metadata: TypeMetadata) : ReferencedRootType() {
    fun toBuilderTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.builderClassName)
    }
}

data class ReferencedEnumType(override val metadata: TypeMetadata) : ReferencedRootType()

data class ComplexType(override val metadata: TypeMetadata, val fields: Map<String, FieldInfo>) : RootType() {
    fun toBuilderTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.builderClassName)
    }

    override fun toReference(): ReferencedComplexType {
        return ReferencedComplexType(metadata)
    }
}

data class EnumType(override val metadata: TypeMetadata, val possibleValues: List<EnumValue>) : RootType() {

    override fun toReference(): ReferencedEnumType {
        return ReferencedEnumType(metadata)
    }
}

data class EnumValue(val value: String, val kDoc: KDoc)

data class ListType(val innerType: ReferencedType) : ReferencedType()

data class MapType(val keyType: ReferencedType, val valueType: ReferencedType) : ReferencedType()

data class EitherType(val firstType: ReferencedType, val secondType: ReferencedType) : ReferencedType()

object AssetOrArchiveType : ReferencedType()

object ArchiveType : ReferencedType()

object JsonType : ReferencedType()

sealed class PrimitiveType(val name: String) : ReferencedType()

object StringType : PrimitiveType("String")
object DoubleType : PrimitiveType("Double")
object IntType : PrimitiveType("Int")
object BooleanType : PrimitiveType("Boolean")

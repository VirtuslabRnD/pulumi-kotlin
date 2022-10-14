package com.virtuslab.pulumikotlin.codegen.step2intermediate

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
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc

data class TypeMetadata(
    val pulumiName: PulumiName,
    val usageKind: UsageKind,
    val kDoc: KDoc,
    val generatedClass: GeneratedClass = GeneratedClass.NormalClass,
) {

    constructor(
        name: String,
        usageKind: UsageKind,
        kDoc: KDoc,
        generatedClass: GeneratedClass,
    ) : this(PulumiName.from(name), usageKind, kDoc, generatedClass)

    constructor(
        name: String,
        usageKind: UsageKind,
        kDoc: KDoc,
    ) : this(PulumiName.from(name), usageKind, kDoc)

    private fun namingFlags(language: LanguageType) =
        NamingFlags(usageKind.depth, usageKind.subject, usageKind.direction, language, generatedClass)

    fun names(language: LanguageType): NameGeneration {
        return NameGeneration(pulumiName, namingFlags(language))
    }
}

data class TypeAndOptionality(val type: ReferencedType, val required: Boolean, val kDoc: KDoc)

sealed class Type {
    abstract fun toTypeName(languageType: LanguageType = LanguageType.Kotlin): TypeName
}

sealed class RootType : Type() {
    abstract val metadata: TypeMetadata

    abstract override fun toTypeName(languageType: LanguageType): ClassName
    abstract fun toReference(): ReferencedRootType
}

sealed class ReferencedRootType : ReferencedType() {
    abstract val metadata: TypeMetadata

    abstract override fun toTypeName(languageType: LanguageType): ClassName
}

sealed class ReferencedType : Type() {
    abstract override fun toTypeName(languageType: LanguageType): TypeName
}

object AnyType : ReferencedType() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return ANY
    }
}

data class ReferencedComplexType(override val metadata: TypeMetadata) : ReferencedRootType() {
    override fun toTypeName(languageType: LanguageType): ClassName {
        val names = metadata.names(languageType)
        return ClassName(names.packageName, names.className)
    }

    fun toBuilderTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.builderClassName)
    }
}

data class ReferencedEnumType(override val metadata: TypeMetadata) : ReferencedRootType() {
    override fun toTypeName(languageType: LanguageType): ClassName {
        val names = metadata.names(languageType)
        return ClassName(names.packageName, names.className)
    }
}

data class ComplexType(override val metadata: TypeMetadata, val fields: Map<String, TypeAndOptionality>) : RootType() {
    override fun toTypeName(languageType: LanguageType): ClassName {
        val names = metadata.names(languageType)
        return ClassName(names.packageName, names.className)
    }

    fun toBuilderTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.builderClassName)
    }

    override fun toReference(): ReferencedComplexType {
        return ReferencedComplexType(metadata)
    }
}

data class EnumType(override val metadata: TypeMetadata, val possibleValues: List<String>) : RootType() {
    override fun toTypeName(languageType: LanguageType): ClassName {
        val names = metadata.names(languageType)
        return ClassName(names.packageName, names.className)
    }

    override fun toReference(): ReferencedEnumType {
        return ReferencedEnumType(metadata)
    }
}

data class ListType(val innerType: ReferencedType) : ReferencedType() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return LIST.parameterizedBy(innerType.toTypeName(languageType))
    }
}

data class MapType(val firstType: ReferencedType, val secondType: ReferencedType) : ReferencedType() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return MAP.parameterizedBy(
            firstType.toTypeName(languageType),
            secondType.toTypeName(languageType),
        )
    }
}

data class EitherType(val firstType: ReferencedType, val secondType: ReferencedType) : ReferencedType() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return Either::class.asTypeName()
            .parameterizedBy(
                firstType.toTypeName(languageType),
                secondType.toTypeName(languageType),
            )
    }
}

object AssetOrArchiveType : ReferencedType() {

    override fun toTypeName(languageType: LanguageType): ClassName {
        return AssetOrArchive::class.asTypeName()
    }
}

object ArchiveType : ReferencedType() {

    override fun toTypeName(languageType: LanguageType): ClassName {
        return Archive::class.asTypeName()
    }
}

sealed class PrimitiveType(val name: String) : ReferencedType() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        require(languageType == LanguageType.Kotlin) { "Types other than ${LanguageType.Kotlin} not expected" }
        return ClassName("kotlin", name)
    }
}

object StringType : PrimitiveType("String")
object DoubleType : PrimitiveType("Double")
object IntType : PrimitiveType("Int")
object BooleanType : PrimitiveType("Boolean")

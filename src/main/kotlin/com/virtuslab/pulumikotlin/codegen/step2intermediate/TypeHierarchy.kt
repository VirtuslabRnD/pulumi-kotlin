package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

data class TypeMetadata(
    val pulumiName: PulumiName,
    val inputOrOutput: InputOrOutput,
    val useCharacteristic: UseCharacteristic,
) {

    constructor(
        pulumiName: PulumiName,
        usage: Usage,
    ) : this(pulumiName, usage.inputOrOutput, usage.useCharacteristic)

    private fun namingFlags(language: LanguageType) =
        NamingFlags(inputOrOutput, useCharacteristic, language)

    fun names(language: LanguageType): NameGeneration {
        return NameGeneration(pulumiName, namingFlags(language))
    }
}

data class TypeWithMetadata(
    val metadata: TypeMetadata,
    val parent: Type,
    val type: Type,
)

data class TypeAndOptionality(val type: Type, val required: Boolean)

sealed class Type {
    abstract fun toTypeName(languageType: LanguageType = LanguageType.Kotlin): TypeName
}

sealed class AutonomousType : Type() {
    abstract val metadata: TypeMetadata

    abstract override fun toTypeName(languageType: LanguageType): ClassName
}

object AnyType : Type() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return ANY
    }
}

data class ComplexType(override val metadata: TypeMetadata, val fields: Map<String, TypeAndOptionality>) :
    AutonomousType() {
    override fun toTypeName(languageType: LanguageType): ClassName {
        val names = metadata.names(languageType)
        return ClassName(names.packageName, names.className)
    }

    fun toBuilderTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.builderClassName)
    }
}

data class EnumType(override val metadata: TypeMetadata, val possibleValues: List<String>) : AutonomousType() {
    override fun toTypeName(languageType: LanguageType): ClassName {
        val names = metadata.names(languageType)
        return ClassName(names.packageName, names.className)
    }
}

data class ListType(val innerType: Type) : Type() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return LIST.parameterizedBy(innerType.toTypeName(languageType))
    }
}

data class MapType(val firstType: Type, val secondType: Type) : Type() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return MAP.parameterizedBy(
            listOf(firstType.toTypeName(languageType), secondType.toTypeName(languageType))
        )
    }
}

data class EitherType(val firstType: Type, val secondType: Type) : Type() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        return ANY // TODO: improve
    }
}

data class PrimitiveType(val name: String) : Type() {
    override fun toTypeName(languageType: LanguageType): TypeName {
        require(languageType == LanguageType.Kotlin) { "Types other than ${LanguageType.Kotlin} not expected" }
        return ClassName("kotlin", name)
    }
}

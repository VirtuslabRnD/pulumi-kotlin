package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*

data class TypeMetadata(
    val pulumiName: PulumiName,
    val inputOrOutput: InputOrOutput,
    val useCharacteristic: UseCharacteristic,
) {

    constructor(
        pulumiName: PulumiName,
        usage: Usage
    ): this(pulumiName, usage.inputOrOutput, usage.useCharacteristic)

    private fun namingFlags(language: LanguageType) =
        NamingFlags(inputOrOutput, useCharacteristic, language)

    fun names(language: LanguageType): NameGeneration {
        return NameGeneration(pulumiName, namingFlags(language))
    }
}

data class TypeWithMetadata(
    val metadata: TypeMetadata,
    val parent: Type,
    val type: Type
)

sealed class Type {
    abstract fun toTypeName(): TypeName
}

sealed class AutonomousType: Type() {
    abstract val metadata: TypeMetadata
}

object AnyType: Type() {
    override fun toTypeName(): TypeName {
        return ANY
    }
}

data class ComplexType(override val metadata: TypeMetadata, val fields: Map<String, Type>) : AutonomousType() {
    override fun toTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.className)
    }
    fun toBuilderTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.builderClassName)
    }
}

data class EnumType(override val metadata: TypeMetadata, val possibleValues: List<String>): AutonomousType() {
    override fun toTypeName(): ClassName {
        val names = metadata.names(LanguageType.Kotlin)
        return ClassName(names.packageName, names.className)
    }
}

data class ListType(val innerType: Type) : Type() {
    override fun toTypeName(): TypeName {
        return LIST.parameterizedBy(innerType.toTypeName())
    }
}

data class MapType(val firstType: Type, val secondType: Type) : Type() {
    override fun toTypeName(): TypeName {
        return MAP.parameterizedBy(
            listOf(firstType.toTypeName(), secondType.toTypeName())
        )
    }
}

data class EitherType(val firstType: Type, val secondType: Type): Type() {
    override fun toTypeName(): TypeName {
        return ANY // TODO: improve
    }
}

data class PrimitiveType(val name: String) : Type() {
    override fun toTypeName(): TypeName {
        return ClassName("kotlin", name)
    }
}
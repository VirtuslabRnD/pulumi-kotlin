package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.TypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType

sealed class FieldType<T : ReferencedType> {
    abstract val type: T
}

data class NormalField<T : ReferencedType>(override val type: T, val mappingCode: MappingCode) : FieldType<T>()

data class OutputWrappedField<T : ReferencedType>(override val type: T) : FieldType<T>()

data class Field<T : ReferencedType>(
    private val name: String,
    val fieldType: FieldType<T>,
    val required: Boolean,
    val overloads: List<FieldType<ReferencedType>> = emptyList(),
    val kDoc: KDoc,
) {
    fun toTypeName(typeNameClashResolver: TypeNameClashResolver): TypeName =
        typeNameClashResolver.toTypeName(fieldType, languageType = LanguageType.Kotlin).copy(nullable = !required)

    fun toNullableTypeName(typeNameClashResolver: TypeNameClashResolver): TypeName =
        typeNameClashResolver.toTypeName(fieldType, languageType = LanguageType.Kotlin).copy(nullable = true)

    fun toKotlinName() = name.replace("$", "")

    fun toJavaName() = BuilderMethodNameEscaper.escape(name)
}

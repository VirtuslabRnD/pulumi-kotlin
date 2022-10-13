package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType

sealed class FieldType<T : ReferencedType> {

    abstract val type: T

    abstract fun toTypeName(): TypeName
}

data class NormalField<T : ReferencedType>(override val type: T, val mappingCode: MappingCode) : FieldType<T>() {
    override fun toTypeName(): TypeName {
        return type.toTypeName()
    }
}

data class OutputWrappedField<T : ReferencedType>(override val type: T) : FieldType<T>() {
    override fun toTypeName(): ParameterizedTypeName {
        return MoreTypes.Java.Pulumi.Output(type.toTypeName())
    }
}

data class Field<T : ReferencedType>(
    val name: String,
    val fieldType: FieldType<T>,
    val required: Boolean,
    val overloads: List<FieldType<*>> = emptyList(),
    val kDoc: KDoc,
)

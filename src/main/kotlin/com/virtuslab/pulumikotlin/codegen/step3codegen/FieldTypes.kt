package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Type

sealed class FieldType<T : Type> {
    abstract fun toTypeName(): TypeName

    abstract val type: T
}

data class NormalField<T : Type>(override val type: T, val mappingCode: MappingCode) : FieldType<T>() {
    override fun toTypeName(): TypeName {
        return type.toTypeName()
    }
}

data class OutputWrappedField<T : Type>(override val type: T) : FieldType<T>() {
    override fun toTypeName(): ParameterizedTypeName {
        return MoreTypes.Java.Pulumi.Output(type.toTypeName())
    }
}

data class Field<T : Type>(
    val name: String,
    val fieldType: FieldType<T>,
    val required: Boolean,
    val overloads: List<FieldType<*>>,
)

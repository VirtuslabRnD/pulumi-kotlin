package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.FieldType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

interface SetterGenerator {
    fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec>
}

data class Setter(
    val name: String,
    val fieldType: FieldType<ReferencedType>,
    val fieldRequired: Boolean,
    val kDoc: KDoc,
) {
    companion object {
        fun from(originalField: Field<ReferencedType>, overload: FieldType<ReferencedType>) =
            Setter(originalField.toKotlinName(), overload, originalField.required, originalField.kDoc)

        fun from(field: Field<ReferencedType>) =
            Setter(field.toKotlinName(), field.fieldType, field.required, field.kDoc)
    }
}

object AllSetterGenerators : SetterGenerator {
    private val generators = listOf(
        BasicSetterGenerator,
        OutputWrappedSetterGenerator,
        ComplexTypeSetterGenerator,
        ListTypeSetterGenerator,
        MapTypeSetterGenerator,
    )

    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        return generators.flatMap { generator -> generator.generate(setter, typeNameClashResolver) }
    }
}

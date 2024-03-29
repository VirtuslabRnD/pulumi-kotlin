package org.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import org.virtuslab.pulumikotlin.codegen.step3codegen.Field
import org.virtuslab.pulumikotlin.codegen.step3codegen.FieldType
import org.virtuslab.pulumikotlin.codegen.step3codegen.KDoc
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

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
        EitherTypeSetterGenerator,
    )

    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        return generators.flatMap { generator -> generator.generate(setter, typeNameClashResolver) }
    }
}

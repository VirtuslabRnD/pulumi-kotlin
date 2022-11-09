package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.mappingCodeBlock
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

object BasicSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        val normalField = setter.fieldType as? NormalField<ReferencedType> ?: return emptyList()

        val name = setter.name
        val required = setter.fieldRequired
        val kDoc = setter.kDoc

        return listOf(
            FunSpec
                .builder(name)
                .addModifiers(SUSPEND)
                .addParameter(
                    "value",
                    typeNameClashResolver.toTypeName(normalField.type, Kotlin).copy(nullable = !required),
                )
                .addCode(mappingCodeBlock(normalField, required, name, "value"))
                .addDocsToBuilderMethod(kDoc, "value")
                .build(),
        )
    }
}

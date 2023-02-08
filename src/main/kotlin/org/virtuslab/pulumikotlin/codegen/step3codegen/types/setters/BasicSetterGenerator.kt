package org.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.SUSPEND
import org.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType.Kotlin
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.mappingCodeBlock
import org.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

object BasicSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        val normalField = setter.fieldType as? NormalField<*> ?: return emptyList()

        val type = if (normalField.type is OptionalType) {
            normalField.type.innerType
        } else {
            normalField.type
        }

        val name = setter.name
        val required = setter.fieldRequired
        val kDoc = setter.kDoc

        return listOf(
            FunSpec
                .builder(name)
                .addModifiers(SUSPEND)
                .addParameter(
                    "value",
                    typeNameClashResolver.toTypeName(type, Kotlin).copy(nullable = !required),
                )
                .addCode(mappingCodeBlock(normalField, required, name, "value"))
                .addDocsToBuilderMethod(kDoc, "value")
                .build(),
        )
    }
}

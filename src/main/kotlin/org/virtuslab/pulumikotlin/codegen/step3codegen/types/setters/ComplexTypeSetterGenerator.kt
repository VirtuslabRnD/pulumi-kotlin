package org.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import org.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.BuilderSettingCodeBlock
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderLambda
import org.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.builderPattern
import org.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import org.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

object ComplexTypeSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        val typedField = setter.fieldType as? NormalField<*> ?: return emptyList()

        val type = if (typedField.type is ReferencedComplexType) {
            typedField.type
        } else if (typedField.type is OptionalType && typedField.type.innerType is ReferencedComplexType) {
            typedField.type.innerType
        } else {
            return emptyList()
        }

        val builderTypeName = type.toBuilderTypeName()

        return listOf(
            builderPattern(
                setter.name,
                builderLambda(builderTypeName),
                setter.kDoc,
                BuilderSettingCodeBlock
                    .create("%T().applySuspend{ argument() }.build()", builderTypeName)
                    .withMappingCode(typedField.mappingCode),
            ),
        )
    }
}

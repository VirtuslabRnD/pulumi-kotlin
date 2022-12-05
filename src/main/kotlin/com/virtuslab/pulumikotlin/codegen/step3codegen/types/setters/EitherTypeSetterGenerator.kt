package com.virtuslab.pulumikotlin.codegen.step3codegen.types.setters

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.OptionalType
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.addDocsToBuilderMethod
import com.virtuslab.pulumikotlin.codegen.step3codegen.KotlinPoetPatterns.mappingCodeBlock
import com.virtuslab.pulumikotlin.codegen.step3codegen.NormalField
import com.virtuslab.pulumikotlin.codegen.step3codegen.TypeNameClashResolver

object EitherTypeSetterGenerator : SetterGenerator {
    override fun generate(setter: Setter, typeNameClashResolver: TypeNameClashResolver): Iterable<FunSpec> {
        val normalField = setter.fieldType as? NormalField<*> ?: return emptyList()

        val type = if (normalField.type is EitherType) {
            normalField.type
        } else if (normalField.type is OptionalType && normalField.type.innerType is EitherType) {
            normalField.type.innerType
        } else {
            return emptyList()
        }

        val firstType = type.firstType
        val secondType = type.secondType

        val firstTypeName = typeNameClashResolver.toTypeName(firstType, LanguageType.Kotlin)
        val secondTypeName = typeNameClashResolver.toTypeName(secondType, LanguageType.Kotlin)

        val name = setter.name
        val kDoc = setter.kDoc

        val firstTypeSetter = createFirstTypeSetter(name, firstTypeName, normalField, secondTypeName, kDoc)

        val secondTypeSetter = createSecondTypeSetter(name, secondTypeName, normalField, firstTypeName, kDoc)

        return listOf(firstTypeSetter, secondTypeSetter)
    }

    private fun createFirstTypeSetter(
        name: String,
        firstTypeName: TypeName,
        normalField: NormalField<*>,
        secondTypeName: TypeName,
        kDoc: KDoc,
    ) = FunSpec
        .builder(name)
        .addParameter("value", firstTypeName)
        .addCode(
            mappingCodeBlock(
                normalField,
                required = true,
                name,
                "Either.ofLeft<%T, %T>(value)",
                firstTypeName,
                secondTypeName,
            ),
        )
        .addDocsToBuilderMethod(kDoc, "value")
        .build()

    private fun createSecondTypeSetter(
        name: String,
        secondTypeName: TypeName,
        normalField: NormalField<*>,
        firstTypeName: TypeName,
        kDoc: KDoc,
    ) = FunSpec
        .builder(name)
        .addParameter("value", secondTypeName)
        .addCode(
            mappingCodeBlock(
                normalField,
                required = true,
                name,
                "Either.ofRight<%T, %T>(value)",
                firstTypeName,
                secondTypeName,
            ),
        )
        .addDocsToBuilderMethod(kDoc, "value")
        .build()
}

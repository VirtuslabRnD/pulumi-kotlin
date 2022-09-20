package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KeywordsEscaper

object ToJava {
    fun toJavaFunction(typeMetadata: TypeMetadata, fields: List<Field<*>>): FunSpec {
        val codeBlocks = fields.map { field ->
            val block = CodeBlock.of(
                ".%N(%N)",
                KeywordsEscaper.escape(field.name),
                field.name,
            )
            val toJavaBlock = CodeBlock.of(".%N(%N?.%N())", KeywordsEscaper.escape(field.name), field.name, "toJava")
            when (val type = field.fieldType.type) {
                AnyType -> block
                is PrimitiveType -> block
                is ComplexType -> toJavaBlock
                is EnumType -> toJavaBlock
                is EitherType -> toJavaBlock
                is ListType -> toJavaBlock
                is MapType -> toJavaBlock
            }
        }

        val names = typeMetadata.names(LanguageType.Java)
        val javaArgsClass = ClassName(names.packageName, names.className)

        return FunSpec.builder("toJava")
            .returns(javaArgsClass)
            .addModifiers(KModifier.OVERRIDE)
            .addCode(CodeBlock.of("return %T.%M()", javaArgsClass, javaArgsClass.member("builder")))
            .apply {
                codeBlocks.forEach { block ->
                    addCode(block)
                }
            }
            .addCode(CodeBlock.of(".build()"))
            .build()
    }
}

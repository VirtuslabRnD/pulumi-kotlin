package com.virtuslab.pulumikotlin.codegen.step3codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AnyType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AssetOrArchiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.EitherType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.JsonType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.LanguageType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ListType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MapType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.NameGeneration
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedEnumType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field

private const val FUNCTION_NAME = "toJava"

object ToJava {
    fun toJavaFunction(fields: List<Field<*>>, names: NameGeneration): FunSpec {
        val codeBlocks = fields.map { field ->
            val block = CodeBlock.of(
                "\n.%N(%N)",
                field.toJavaName(),
                field.toKotlinName(),
            )
            val toJavaBlock = CodeBlock.of(
                ".%N(%N${if (field.required) "" else "?"}.%N())",
                field.toJavaName(),
                field.toKotlinName(),
                FUNCTION_NAME,
            )
            when (field.fieldType.type) {
                is AnyType -> block
                is PrimitiveType -> block
                is EitherType -> toJavaBlock
                is ListType -> toJavaBlock
                is MapType -> toJavaBlock
                is ReferencedComplexType -> toJavaBlock
                is ReferencedEnumType -> toJavaBlock
                is AssetOrArchiveType, is ArchiveType -> block
                is JsonType -> toJavaBlock
            }
        }

        val javaArgsClass = ClassName(names.packageName, names.className)

        return FunSpec.builder(FUNCTION_NAME)
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

    fun toJavaEnumFunction(typeMetadata: TypeMetadata): FunSpec {
        val javaClass = typeMetadata.names(LanguageType.Java).kotlinPoetClassName

        return FunSpec.builder(FUNCTION_NAME)
            .addModifiers(KModifier.OVERRIDE)
            .returns(javaClass)
            .addStatement("return %T.valueOf(this.name)", javaClass)
            .build()
    }
}

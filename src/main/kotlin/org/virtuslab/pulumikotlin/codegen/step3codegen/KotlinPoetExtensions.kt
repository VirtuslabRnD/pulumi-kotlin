package org.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.virtuslab.pulumikotlin.codegen.expressions.Code

object KotlinPoetExtensions {
    fun TypeSpec.Builder.addFunctions(vararg funSpecs: FunSpec) = addFunctions(funSpecs.toList())

    fun TypeSpec.Builder.addProperties(vararg propertySpecs: PropertySpec) = addProperties(propertySpecs.toList())

    fun FileSpec.Builder.addImport(memberName: MemberName) = addImport(memberName.packageName, memberName.simpleName)

    fun FileSpec.Builder.addImports(vararg memberNames: MemberName) =
        apply {
            memberNames.forEach {
                addImport(it)
            }
        }

    fun FileSpec.Builder.addTypes(vararg typeSpecs: TypeSpec) = addTypes(typeSpecs.toList())

    fun CodeBlock.Builder.add(code: Code) = add(code.toCodeBlock().toKotlinPoetCodeBlock())
}

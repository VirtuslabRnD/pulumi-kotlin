package org.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import org.virtuslab.pulumikotlin.codegen.expressions.Assignment
import org.virtuslab.pulumikotlin.codegen.expressions.CustomExpression
import org.virtuslab.pulumikotlin.codegen.expressions.Expression
import org.virtuslab.pulumikotlin.codegen.expressions.add
import org.virtuslab.pulumikotlin.codegen.expressions.callLet
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ReferencedComplexType

typealias MappingCode = (Expression) -> Expression

object KotlinPoetPatterns {

    /**
     * Example output:
     *
     * ```
     * List<suspend InnerType.() -> Unit>
     * ```
     */
    fun listOfLambdas(innerType: TypeName): TypeName =
        LIST.parameterizedBy(builderLambda(innerType))

    /**
     * @see listOfLambdas
     */
    fun listOfLambdas(innerType: ComplexType): TypeName =
        listOfLambdas(innerType.toBuilderTypeName())

    /**
     * @see listOfLambdas
     */
    fun listOfLambdas(innerType: ReferencedComplexType): TypeName =
        listOfLambdas(innerType.toBuilderTypeName())

    /**
     * Example output:
     *
     * ```
     * suspend InnerType.() -> Unit
     * ```
     */
    fun builderLambda(innerType: ComplexType): TypeName =
        builderLambda(innerType.toBuilderTypeName())

    /**
     * @see builderLambda
     */
    fun builderLambda(innerType: TypeName): TypeName =
        LambdaTypeName
            .get(receiver = innerType, returnType = UNIT)
            .copy(suspending = true)

    /**
     * @see builderLambda
     */
    fun builderLambda(innerType: ReferencedComplexType): TypeName =
        builderLambda(innerType.toBuilderTypeName())

    data class BuilderSettingCodeBlock(val mappingCode: MappingCode? = null, val code: String, val args: List<Any?>) {
        fun withMappingCode(mappingCode: MappingCode): BuilderSettingCodeBlock {
            return copy(mappingCode = mappingCode)
        }

        fun toCodeBlock(fieldToSetName: String): CodeBlock {
            return if (mappingCode != null) {
                CodeBlock.builder()
                    .addStatement("val toBeMapped = $code", *args.toTypedArray())
                    .add(Assignment("mapped", mappingCode.invoke(CustomExpression("toBeMapped"))))
                    .addStatement("")
                    .addStatement("this.%N = mapped", fieldToSetName)
                    .build()
            } else {
                CodeBlock.builder()
                    .addStatement("this.%N = $code", fieldToSetName, *args.toTypedArray())
                    .build()
            }
        }

        companion object {
            fun create(code: String, vararg args: Any?) = BuilderSettingCodeBlock(null, code, args.toList())
        }
    }

    /**
     * Example output:
     *
     * ```
     * suspend fun name(vararg argument: ParameterType): Unit {
     *    val toBeMapped = something.toList().somethingElse()
     *    val mapped = mappingCode(toBeMapped)
     *    this.field = mapped
     * }
     * ```
     */
    fun builderPattern(
        name: String,
        parameterType: TypeName,
        kDoc: KDoc,
        codeBlock: BuilderSettingCodeBlock,
        parameterModifiers: List<KModifier> = emptyList(),
    ): FunSpec {
        return FunSpec
            .builder(name)
            .addModifiers(SUSPEND)
            .addParameter(
                "argument",
                parameterType,
                parameterModifiers,
            )
            .addCode(codeBlock.toCodeBlock(name))
            .addDocsToBuilderMethod(kDoc, "argument")
            .build()
    }

    /**
     * Example output:
     *
     * ```
     * val toBeMapped = something.toList().somethingElse()
     * val mapped = mappingCode(toBeMapped)
     * this.field = mapped
     * ```
     */
    fun mappingCodeBlock(
        field: NormalField<*>,
        required: Boolean,
        name: String,
        code: String,
        vararg args: Any?,
    ): CodeBlock {
        val expression = CustomExpression("toBeMapped").callLet(!required) { arg -> field.mappingCode(arg) }
        return CodeBlock.builder()
            .addStatement("val toBeMapped = $code", *args)
            .add(Assignment("mapped", expression))
            .addStatement("")
            .addStatement("this.%N = mapped", name)
            .build()
    }

    fun FunSpec.Builder.addDocsToBuilderMethod(kDoc: KDoc, paramName: String) = apply {
        addDocs("@param $paramName ${kDoc.description.orEmpty()}")
        addDeprecationWarningIfAvailable(kDoc)
    }

    fun FileSpec.Builder.addStandardSuppressAnnotations() = addAnnotation(
        AnnotationSpec.builder(Suppress::class)
            .addMember("\"NAME_SHADOWING\", \"DEPRECATION\"")
            .build(),
    )
}

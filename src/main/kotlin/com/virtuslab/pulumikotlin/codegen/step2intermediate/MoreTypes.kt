package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.pulumi.kotlin.ConvertibleToJava
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

object MoreTypes {

    object Kotlin {
        object Pulumi {
            fun toJavaMethod(): MemberName {
                return MemberName("com.pulumi.kotlin", "toJava")
            }

            fun pulumiDslMarkerAnnotation(): ClassName {
                return ClassName("com.pulumi.kotlin", "PulumiTagMarker")
            }

            fun toKotlinMethod(): MemberName {
                return MemberName("com.pulumi.kotlin", "toKotlin")
            }

            fun applySuspendExtensionMethod(): MemberName {
                return MemberName("com.pulumi.kotlin", "applySuspend")
            }

            fun applyValueExtensionMethod(): MemberName {
                return MemberName("com.pulumi.kotlin", "applyValue")
            }

            fun convertibleToJavaClass(type: TypeName): ParameterizedTypeName =
                ConvertibleToJava::class.asClassName().parameterizedBy(type)

            fun customArgsClass(): ClassName {
                return ClassName("com.pulumi.kotlin", "CustomArgs")
            }

            fun customArgsBuilderClass(): ClassName {
                return ClassName("com.pulumi.kotlin", "CustomArgsBuilder")
            }
        }

        fun coroutinesFutureAwaitExtensionMethod(): MemberName {
            return MemberName("kotlinx.coroutines.future", "await")
        }

        fun pairClass(leftType: TypeName, rightType: TypeName): ParameterizedTypeName {
            return ClassName("kotlin", "Pair").parameterizedBy(leftType, rightType)
        }
    }

    object Java {
        object Pulumi {
            fun outputOfMethod(): MemberName =
                outputClass().member("of")

            fun outputClass(): ClassName {
                return ClassName("com.pulumi.core", "Output")
            }

            fun outputClass(type: TypeName): ParameterizedTypeName {
                return ClassName("com.pulumi.core", "Output").parameterizedBy(type)
            }
        }
    }
}

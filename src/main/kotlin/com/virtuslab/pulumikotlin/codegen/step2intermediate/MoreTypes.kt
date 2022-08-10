package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

object MoreTypes {

    object Kotlin {
        object Pulumi {
            fun toJava(): MemberName {
                return MemberName("com.pulumi.kotlin", "toJava")
            }

            fun applySuspend(): MemberName {
                return MemberName("com.pulumi.kotlin", "applySuspend")
            }

            fun ConvertibleToJava(type: TypeName): ParameterizedTypeName =
                ClassName("com.pulumi.kotlin", "ConvertibleToJava").parameterizedBy(type)
        }
        fun Pair(leftType: TypeName, rightType: TypeName): ParameterizedTypeName {
            return ClassName("kotlin", "Pair").parameterizedBy(leftType, rightType)
        }
    }

    object Java {
        object Pulumi {
            fun Output(): ClassName {
                return ClassName("com.pulumi.core", "Output")
            }
            fun Output(type: TypeName): ParameterizedTypeName {
                return ClassName("com.pulumi.core", "Output").parameterizedBy(type)
            }
        }

    }

}
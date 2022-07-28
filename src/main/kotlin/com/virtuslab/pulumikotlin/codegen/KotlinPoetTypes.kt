package com.virtuslab.pulumikotlin.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

object MoreTypes {

    object Kotlin {
        fun Pair(leftType: TypeName, rightType: TypeName): ParameterizedTypeName {
            return ClassName("kotlin", "Pair").parameterizedBy(leftType, rightType)
        }
    }

    object Pulumi {
        fun Output(): ClassName {
            return ClassName("com.pulumi.core", "Output")
        }
        fun Output(type: TypeName): ParameterizedTypeName {
            return ClassName("com.pulumi.core", "Output").parameterizedBy(type)
        }
    }
}
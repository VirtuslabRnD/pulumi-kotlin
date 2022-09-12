package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType

object KotlinPoetTypes {

    fun listOfLambdas(innerType: TypeName): TypeName {
        return LIST.parameterizedBy(builderLambda(innerType))
    }

    fun listOfLambdas(innerType: ComplexType): TypeName {
        return listOfLambdas(innerType.toBuilderTypeName())
    }

    fun builderLambda(innerType: ComplexType): TypeName = builderLambda(innerType.toBuilderTypeName())

    fun builderLambda(innerType: TypeName): TypeName {
        return LambdaTypeName
            .get(receiver = innerType, returnType = UNIT)
            .copy(suspending = true)
    }

}

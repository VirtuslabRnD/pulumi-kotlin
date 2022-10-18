package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.GeneratedClass.NormalClass
import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc

data class UsageKind(val depth: Depth, val subject: Subject, val direction: Direction) {
    fun toNested() = copy(depth = Nested)
}

enum class Direction {
    Input, Output
}

enum class Depth {
    Root, Nested
}

enum class Subject {
    Function, Resource
}

enum class LanguageType {
    Kotlin, Java
}

enum class GeneratedClass {
    EnumClass, NormalClass
}

data class NamingFlags(
    val depth: Depth,
    val subject: Subject,
    val direction: Direction,
    val language: LanguageType,
    val generatedClass: GeneratedClass = NormalClass,
)

data class ResourceType(
    val name: PulumiName,
    val argsType: ReferencedComplexType,
    val outputFields: List<Field<*>>,
    val kDoc: KDoc,
)

data class FunctionType(
    val name: PulumiName,
    val argsType: RootType,
    val outputType: ReferencedRootType,
    val kDoc: KDoc,
)

data class IntermediateRepresentation(
    val resources: List<ResourceType>,
    val functions: List<FunctionType>,
    val types: List<RootType>,
)

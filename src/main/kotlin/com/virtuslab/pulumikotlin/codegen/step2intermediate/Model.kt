package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step3codegen.Field
import com.virtuslab.pulumikotlin.codegen.step3codegen.KDoc

data class Usage(
    val depth: Depth,
    val subject: Subject,
    val direction: Direction,
) {
    @Deprecated("use Usage(Level, Subject, Direction) constructor")
    constructor(direction: Direction, useCharacteristic: UseCharacteristic) : this(
        useCharacteristic.toSubjectAndLevel().second,
        useCharacteristic.toSubjectAndLevel().first,
        direction,
    )

    val useCharacteristic: UseCharacteristic
        get() = UseCharacteristic.from(depth, subject)

    fun toNested() = copy(depth = Depth.Nested)
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

@Deprecated("use Level and Subject")
enum class UseCharacteristic {
    FunctionNested, ResourceNested, ResourceRoot, FunctionRoot;

    fun toSubjectAndLevel(): Pair<Subject, Depth> =
        when (this) {
            FunctionNested -> Pair(Subject.Function, Depth.Nested)
            FunctionRoot -> Pair(Subject.Function, Depth.Root)
            ResourceNested -> Pair(Subject.Resource, Depth.Nested)
            ResourceRoot -> Pair(Subject.Resource, Depth.Root)
        }

    companion object {
        fun from(depth: Depth, subject: Subject) =
            when (Pair(depth, subject)) {
                Pair(Depth.Root, Subject.Function) -> FunctionRoot
                Pair(Depth.Root, Subject.Resource) -> ResourceRoot
                Pair(Depth.Nested, Subject.Resource) -> ResourceNested
                Pair(Depth.Nested, Subject.Function) -> FunctionNested
                else -> error("?")
            }
    }
}

enum class LanguageType {
    Kotlin, Java
}

enum class GeneratedClass {
    EnumClass, NormalClass
}

data class NamingFlags(
    val direction: Direction,
    val usage: UseCharacteristic,
    val language: LanguageType,
    val generatedClass: GeneratedClass = GeneratedClass.NormalClass,
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

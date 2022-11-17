package com.virtuslab.pulumikotlin.codegen.step3codegen

import javax.lang.model.SourceVersion

object BuilderMethodNameEscaper {

    private val javaObjectFunctionNames = java.lang.Object::class
        .java
        .declaredMethods
        .map { it.name }
        .toSet()

    fun escape(name: String): String {
        return if (SourceVersion.isKeyword(name) || javaObjectFunctionNames.contains(name) || name == "builder") {
            name + "_"
        } else {
            name
        }
    }
}

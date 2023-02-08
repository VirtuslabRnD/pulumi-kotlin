package org.virtuslab.pulumikotlin.codegen.step3codegen

import javax.lang.model.SourceVersion

object ReservedWordEscaper {

    private val javaObjectFunctionNames = java.lang.Object::class
        .java
        .declaredMethods
        .map { it.name }
        .toSet()

    fun escapeBuilderMethodName(name: String): String {
        return if (SourceVersion.isKeyword(name) || javaObjectFunctionNames.contains(name) || name == "builder") {
            name + "_"
        } else {
            name
        }
    }

    fun escapeKeyword(name: String): String {
        return if (SourceVersion.isKeyword(name)) {
            name + "_"
        } else {
            name
        }
    }
}

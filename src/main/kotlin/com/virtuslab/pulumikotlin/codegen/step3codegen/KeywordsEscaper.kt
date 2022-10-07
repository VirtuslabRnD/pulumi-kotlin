package com.virtuslab.pulumikotlin.codegen.step3codegen

object KeywordsEscaper {
    private val keywords = setOf("public", "interface", "default", "clone", "transient", "private", "protected")

    fun escape(name: String): String {
        return if (keywords.contains(name)) {
            name + "_"
        } else {
            name
        }
    }
}

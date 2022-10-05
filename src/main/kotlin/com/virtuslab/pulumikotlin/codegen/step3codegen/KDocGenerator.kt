package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.AnnotationSpec

object KDocGenerator {

    fun addKDoc(kDocBuilder: KDocBuilder, kDoc: KDoc) {
        if (kDoc.description != null) {
            addKDoc(kDocBuilder, kDoc.description)
        }
    }

    fun addKDoc(kDocBuilder: KDocBuilder, kDoc: String) {
        val howManyCommentsToOpen = "(?<!/)\\*/".toRegex().findAll(kDoc).count()
        val howManyCommentsToClose = "(?<!\\*)/\\*".toRegex().findAll(kDoc).count()

        kDocBuilder.apply(
            "${" /*".repeat(howManyCommentsToOpen)}${kDoc}${"*/".repeat(howManyCommentsToClose)}"
                .replace(Regex("\\s*\n"), "\n")
                .replace(" ", "·")
                .replace("%", "%%"),
        )
    }

    fun addDeprecationWarning(annotationBuilder: AnnotationBuilder, kDoc: KDoc) {
        if (kDoc.deprecationMessage != null) {
            annotationBuilder.apply(
                AnnotationSpec.builder(Deprecated::class)
                    .addMember("message = \"\"\"\n${kDoc.deprecationMessage}\n\"\"\"")
                    .build(),
            )
        }
    }

    fun interface KDocBuilder {
        fun apply(format: String, vararg args: Any)
    }

    fun interface AnnotationBuilder {
        fun apply(annotationSpec: AnnotationSpec)
    }
}

data class KDoc(val description: String?, val deprecationMessage: String?)

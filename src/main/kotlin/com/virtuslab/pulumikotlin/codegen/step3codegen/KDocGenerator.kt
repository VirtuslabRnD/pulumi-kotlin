package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.AnnotationSpec
import kotlin.text.RegexOption.DOT_MATCHES_ALL

private const val EXAMPLES_HEADER_REGEX = "\\{\\{% examples %}}\\n(.*?)\\{\\{% example %}}(.*?)\\{\\{% /examples %}}"
private const val EXAMPLES_HEADER_REGEX_GROUP_NUMBER = 1
private const val CODE_SNIPPET_REGEX = "\\{\\{% example %}}(.*?)\\{\\{% /example %}}"
private const val CODE_SNIPPET_REGEX_GROUP_NUMBER = 1
private const val JAVA_CODE_SNIPPET_REGEX = """(.*?)(```\w+\n.*?```\n)*?(```java\n(.*?)```\n)(```\w+\n.*?```\n)*"""
private const val JAVA_CODE_SNIPPET_REGEX_GROUP_NUMBER = 3
private const val EXAMPLE_HEADER_REGEX = """(.*?)```\w+\n.+?```"""
private const val EXAMPLE_HEADER_REGEX_GROUP_NUMBER = 1
private const val EXAMPLES_SECTION_REGEX = "\\{\\{% examples %}}(.*?)\\{\\{% /examples %}}"

object KDocGenerator {

    fun addKDoc(kDocBuilder: KDocBuilder, kDoc: KDoc) {
        if (kDoc.description != null) {
            addKDoc(kDocBuilder, kDoc.description)
        }
    }

    fun addKDoc(kDocBuilder: KDocBuilder, kDoc: String) {
        val examplesHeader = EXAMPLES_HEADER_REGEX.toRegex(DOT_MATCHES_ALL)
            .find(kDoc)
            ?.groupValues
            ?.get(EXAMPLES_HEADER_REGEX_GROUP_NUMBER)
            .orEmpty()

        val examples = CODE_SNIPPET_REGEX.toRegex(DOT_MATCHES_ALL)
            .findAll(kDoc)
            .map { it.groupValues[CODE_SNIPPET_REGEX_GROUP_NUMBER] }
            .map { example ->
                val javaSnippet = JAVA_CODE_SNIPPET_REGEX.toRegex(DOT_MATCHES_ALL)
                    .find(example)
                if (javaSnippet != null) {
                    val groups = javaSnippet.groupValues
                    "${groups[EXAMPLE_HEADER_REGEX_GROUP_NUMBER]}${groups[JAVA_CODE_SNIPPET_REGEX_GROUP_NUMBER]}"
                } else {
                    val exampleHeader = EXAMPLE_HEADER_REGEX.toRegex(DOT_MATCHES_ALL)
                        .find(example)
                        ?.groupValues
                        ?.get(EXAMPLE_HEADER_REGEX_GROUP_NUMBER)
                    if (exampleHeader != null) {
                        "$exampleHeader\nNo Java example available."
                    } else {
                        ""
                    }
                }
            }
            .joinToString("")

        val trimmedDocs = kDoc.replace(
            EXAMPLES_SECTION_REGEX.toRegex(DOT_MATCHES_ALL),
            "$examplesHeader\n$examples",
        )

        val howManyCommentsToOpen = "(?<!/)\\*/"
            .toRegex()
            .findAll(trimmedDocs)
            .count()
        val howManyCommentsToClose = "(?<!\\*)/\\*"
            .toRegex()
            .findAll(trimmedDocs)
            .count()

        kDocBuilder.apply(
            (
                " /*".repeat(howManyCommentsToOpen) +
                    (if (howManyCommentsToOpen > 0) "\n" else "") +
                    trimmedDocs +
                    (if (howManyCommentsToClose > 0) "\n" else "") +
                    "*/".repeat(howManyCommentsToClose)
                )
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

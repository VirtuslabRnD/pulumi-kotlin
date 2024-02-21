package org.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

private const val EXAMPLES_HEADER_REGEX = """\{\{% examples %}}\n(.*?)\{\{% example %}}(.*?)\{\{% /examples %}}"""
private const val EXAMPLES_HEADER_REGEX_GROUP_NUMBER = 1
private const val CODE_SNIPPET_REGEX = """\{\{% example %}}(.*?)\{\{% /example %}}"""
private const val CODE_SNIPPET_REGEX_GROUP_NUMBER = 1
private const val JAVA_CODE_SNIPPET_REGEX = """(.*?)(```\w+\n.*?```\n)*?(```java\n(.*?)```\n)(```\w+\n.*?```\n)*"""
private const val JAVA_CODE_SNIPPET_REGEX_GROUP_NUMBER = 3
private const val EXAMPLE_HEADER_REGEX = """(.*?)```\w+\n.+?```"""
private const val EXAMPLE_HEADER_REGEX_GROUP_NUMBER = 1

private const val MARKDOWN_HYPERLINK = "\\[.*]"

private const val MARKDOWN_EMPTY_HEADER = "^[#\\s]*#[#\\s]*\$"

private const val FULL_STOP = "."
private const val FULL_STOP_HTML_CODE = "&#46;"

fun PropertySpec.Builder.addDocsIfAvailable(kDoc: KDoc) = apply {
    addDocsIfAvailable(
        { format, args -> addKdoc(format, args) },
        kDoc,
    )
}

fun TypeSpec.Builder.addDocsIfAvailable(kDoc: KDoc) = apply {
    addDocsIfAvailable(
        { format, args -> addKdoc(format, args) },
        kDoc,
    )
}

fun FunSpec.Builder.addDocs(kDoc: String) = apply {
    addDocs(
        { format, args -> addKdoc(format, args) },
        kDoc,
    )
}

fun TypeSpec.Builder.addDocs(kDoc: String) = apply {
    addDocs(
        { format, args -> addKdoc(format, args) },
        kDoc,
    )
}

private fun addDocsIfAvailable(kDocBuilder: KDocBuilder, kDoc: KDoc) {
    if (kDoc.description != null) {
        addDocs(kDocBuilder, kDoc.description)
    }
}

private fun addDocs(kDocBuilder: KDocBuilder, kDoc: String) {
    val examples = getTrimmedExamplesBlock(kDoc)

    val trimmedDocs = kDoc
        .replace(MARKDOWN_HYPERLINK.toRegex()) {
            it.value.replace(FULL_STOP, FULL_STOP_HTML_CODE)
        }
        .replace(EXAMPLES_HEADER_REGEX.toRegex(DOT_MATCHES_ALL)) {
            it.groupValues[EXAMPLES_HEADER_REGEX_GROUP_NUMBER] + "\n$examples"
        }
        .replace(MARKDOWN_EMPTY_HEADER.toRegex(MULTILINE), "")

    kDocBuilder.apply(
        addCommentClosingsAndOpenings(trimmedDocs)
            .replace(Regex("\\s*\n"), "\n")
            .replace(" ", "·")
            .replace("%", "%%"),
    )
}

private fun getTrimmedExamplesBlock(kDoc: String) = CODE_SNIPPET_REGEX.toRegex(DOT_MATCHES_ALL)
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

private fun addCommentClosingsAndOpenings(trimmedDocs: String): String {
    val howManyCommentsToOpen = trimmedDocs.countOccurrences("\\*/", "/")
    val howManyCommentsToClose = trimmedDocs.countOccurrences("/\\*", "\\*")

    return " /*".repeat(howManyCommentsToOpen) +
        (if (howManyCommentsToOpen > 0) "\n" else "") +
        trimmedDocs +
        (if (howManyCommentsToClose > 0) "\n" else "") +
        "*/".repeat(howManyCommentsToClose)
}

private fun String.countOccurrences(substring: String, notPrecededBy: String) =
    "(?<!$notPrecededBy)($substring)+".toRegex()
        .findAll(this)
        .map { it.groupValues[0] }
        .map { substring.toRegex().findAll(it).count() }
        .sum()

fun FunSpec.Builder.addDeprecationWarningIfAvailable(kDoc: KDoc) = apply {
    addDeprecationWarningIfAvailable(
        { annotationSpec -> this.addAnnotation(annotationSpec) },
        kDoc,
    )
}

fun PropertySpec.Builder.addDeprecationWarningIfAvailable(kDoc: KDoc) = apply {
    addDeprecationWarningIfAvailable(
        { annotationSpec -> this.addAnnotation(annotationSpec) },
        kDoc,
    )
}

fun TypeSpec.Builder.addDeprecationWarningIfAvailable(kDoc: KDoc) = apply {
    addDeprecationWarningIfAvailable(
        { annotationSpec -> this.addAnnotation(annotationSpec) },
        kDoc,
    )
}

private fun addDeprecationWarningIfAvailable(annotationBuilder: AnnotationBuilder, kDoc: KDoc) {
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

data class KDoc(val description: String?, val deprecationMessage: String?)

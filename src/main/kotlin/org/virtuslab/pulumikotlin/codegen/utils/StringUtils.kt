package org.virtuslab.pulumikotlin.codegen.utils

import java.util.Locale

fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.decapitalize() =
    replaceFirstChar { it.lowercase(Locale.getDefault()) }

/**
 * Example:
 * ```kt
 * "very very very very long string".shorten(20, "....") // "very ver....g string"
 * ```
 */
fun String.shorten(desiredLength: Int = 20, fillWith: String = "<<shortened>>"): String {
    if (desiredLength <= fillWith.length || desiredLength >= length) {
        return this
    }

    val contentDesiredLength = desiredLength - fillWith.length
    val leftSideLength = contentDesiredLength / 2
    val rightSideLength = contentDesiredLength - leftSideLength

    val left = substring(0, leftSideLength)
    val right = substring(length - rightSideLength)

    return left + fillWith + right
}

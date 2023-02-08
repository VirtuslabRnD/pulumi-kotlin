package org.virtuslab.pulumikotlin.codegen.step3codegen.types

import org.virtuslab.pulumikotlin.codegen.step3codegen.ReservedWordEscaper
import org.virtuslab.pulumikotlin.codegen.utils.letIf

object EnumValueGenerator {

    private val commonEnumNameReplacements = mapOf(
        "*" to "Asterisk",
        "0" to "Zero",
        "1" to "One",
        "2" to "Two",
        "3" to "Three",
        "4" to "Four",
        "5" to "Five",
        "6" to "Six",
        "7" to "Seven",
        "8" to "Eight",
        "9" to "Nine",
    )

    /**
     * This function is based on the `ident::MakeSafeEnumName` function from the Pulumi library in Go.
     */
    fun makeSafeEnumName(enumName: String): String {
        return enumName.expandShortEnumName()
            .letIf({ it.length == 1 && !it[0].isLegalIdentifierStart() }) {
                throw IllegalStateException("Cannot construct an enum with name `$enumName`")
            }
            .makeValid()
            .titlecaseString()
            .removeExcessUnderscores()
    }

    /**
     * This function is based on the `utilities::ExpandShortEnumName` function from the Pulumi library in Go.
     */
    private fun String.expandShortEnumName() = commonEnumNameReplacements.getOrDefault(this, this)

    /**
     * This function is based on the `ident::Ident::makeValid` function from the Pulumi library in Go.
     */
    private fun String.makeValid(): String {
        val builder = StringBuilder()
        for ((index, value) in withIndex()) {
            if (index == 0 && value == '$') {
                builder.append(value)
            } else if (value == '-') {
                continue
            } else if (!value.isLegalIdentifierPart()) {
                builder.append("_")
            } else {
                if (index == 0 && !value.isLegalIdentifierStart()) {
                    builder.append("_")
                }
                builder.append(value)
            }
        }
        return ReservedWordEscaper.escapeKeyword(builder.toString())
    }

    /**
     * This function is based on the `strings::Title` function from Go.
     */
    private fun String.titlecaseString(): String {
        val titlecaseStringBuilder = StringBuilder()
        var previous = ' '
        for (index in indices) {
            if (previous.isSeparator()) {
                titlecaseStringBuilder.append(this[index].titlecaseChar())
            } else {
                titlecaseStringBuilder.append(this[index])
            }
            previous = this[index]
        }
        return titlecaseStringBuilder.toString()
    }

    private fun String.removeExcessUnderscores(): String = replace("_+".toRegex(), "_")

    /**
     * This function is based on the `strings::isSeparator` function from Go.
     */
    @Suppress("MagicNumber")
    private fun Char.isSeparator(): Boolean {
        return if (code <= 0x7F) {
            when {
                this in '0'..'9' -> false
                this in 'a'..'z' -> false
                this in 'A'..'Z' -> false
                this == '_' -> false
                else -> true
            }
        } else if (isLetter() || isDigit()) {
            false
        } else {
            isWhitespace()
        }
    }

    /**
     * This function is based on the `ident::isLegalIdentifierStart` function from the Pulumi library in Go.
     */
    private fun Char.isLegalIdentifierStart(): Boolean = this == '$' || this == '_' || this.isLetter()

    /**
     * This function is based on the `ident::isLegalIdentifierPart` function from the Pulumi library in Go.
     */
    private fun Char.isLegalIdentifierPart(): Boolean = this == '$' || this == '_' || this.isLetterOrDigit()
}

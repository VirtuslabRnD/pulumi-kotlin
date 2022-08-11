package com.virtuslab.pulumikotlin.scripts

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.FunctionsMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ResourcesMap
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import java.io.File


/**
 * This is to understand all the possible names structures in Pulumi provider's schema. For example, AWS classic can only have names with the following structure (notice `:` and `/`):
 *
 * - aws-native:acmpca:Certificate
 * - aws:accessanalyzer/analyzer:Analyzer
 *
 * (It seems obvious now, but I wasn't so sure before writing this script. I'm also not sure about the other providers like kubernetes.)
 */

@kotlinx.serialization.Serializable
data class Example(val value: String, val from: String)

@kotlinx.serialization.Serializable
data class CountWithExamples(val count: Int = 0, val examples: List<Example> = emptyList()) {
    fun withOneMore(example: Example): CountWithExamples {
        return copy(
            count = count + 1,
            examples = (examples + example).take(3)
        )
    }
}

val jsonOutput = Json {
    prettyPrint = true
}

fun main() {
    val files = listOf(
        "/Users/mfudala/workspace/pulumi-kotlin/src/main/resources/schema.json",
        "/Users/mfudala/workspace/pulumi-kotlin/src/main/resources/schema-aws-classic.json"
    )

    val stats = mutableMapOf<String, CountWithExamples>().withDefault { _ ->
        CountWithExamples()
    }

    val json = Json
    files.forEach { file ->
        val decoded = json.parseToJsonElement(File(file).readText())


        val types = decoded.decodeSection<TypesMap>("types")
        val functions = decoded.decodeSection<FunctionsMap>("functions")
        val resources = decoded.decodeSection<ResourcesMap>("resources")

        fun extractPattern(string: String): String {
            return string.replace(Regex("[a-zA-Z0-9]|-"), "")
        }

        val names = resources.map { "r" to it.key } + types.map { "t" to it.key } + functions.map { "f" to it.key }
        names.forEach { (type, name) ->
            stats[extractPattern(name)] =
                stats[extractPattern(name)]?.withOneMore(Example(name, type)) ?: CountWithExamples()
        }
    }

    val moreStats = mutableMapOf<String, CountWithExamples>().withDefault { _ ->
        CountWithExamples()
    }

    val regexAndRuleToTest = listOf(
        Regex("(.+?):(.+?)/(.+?):(.+?)") to { m: MatchResult ->
            m.groupValues.get(4).lowercase() == m.groupValues.get(3).lowercase()
        }
    )

    files.forEach { file ->
        val decoded = json.parseToJsonElement(File(file).readText())


        val types = decoded.decodeSection<TypesMap>("types")
        val functions = decoded.decodeSection<FunctionsMap>("functions")
        val resources = decoded.decodeSection<ResourcesMap>("resources")

        fun extractPattern(string: String): String {
            return regexAndRuleToTest
                .asSequence()
                .map { regex ->
                    regex.first.matchEntire(string)
                        ?.let { "yes" to (if (regex.second(it)) "yes" else "no") }
                        ?: ("no" to "no")
                }
                .withIndex()
                .map { it.index.toString() + ":" + it.value.first + "," + it.value.second }
                .joinToString(",")
        }

        val names = resources.map { "r" to it.key } + types.map { "t" to it.key } + functions.map { "f" to it.key }
        names.forEach { (type, name) ->
            moreStats[extractPattern(name)] =
                moreStats[extractPattern(name)]?.withOneMore(Example(name, type)) ?: CountWithExamples()
        }
    }

    println(jsonOutput.encodeToString(stats))

    println(jsonOutput.encodeToString(moreStats))
}


inline fun <reified T> JsonElement.decodeSection(section: String) =
    Json.decodeFromJsonElement<T>(this.jsonObject[section]!!)

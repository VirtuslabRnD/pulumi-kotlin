package org.virtuslab.pulumikotlin.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.FunctionsMap
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.ResourcesMap
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.TypesMap
import java.io.File

fun main(args: Array<String>) {
    ComputePulumiSchemaNamingStatsScript().main(args)
}

/**
 * This is to understand all the possible names structures in Pulumi provider's schema.
 *
 * For example, AWS classic can only have names with the following structure (notice `:` and `/`):
 *
 * - aws-native:acmpca:Certificate
 * - aws:accessanalyzer/analyzer:Analyzer
 *
 * It seems obvious now, but I wasn't so sure before writing this script.
 * I'm also not sure about the other providers like kubernetes.
 */
class ComputePulumiSchemaNamingStatsScript : CliktCommand() {

    private val logger = KotlinLogging.logger {}

    private val schemaPaths: List<String> by option()
        .split(",")
        .required()
        .validate {
            it.forEach { path -> require(File(path).exists()) { "File $it does not exist" } }
        }

    override fun run() {
        val json = Json {
            prettyPrint = true
        }

        val schemaFiles = schemaPaths.map { File(it) }

        val stats = mutableMapOf<String, CountWithExamples>().withDefault { _ ->
            CountWithExamples()
        }

        schemaFiles.forEach { file ->
            val decoded = json.parseToJsonElement(file.readText())

            val types: TypesMap = decoded.decodeMap("types")
            val functions: FunctionsMap = decoded.decodeMap("functions")
            val resources: ResourcesMap = decoded.decodeMap("resources")

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
                m.groupValues[4].lowercase() == m.groupValues[3].lowercase()
            },
        )

        schemaFiles.forEach { file ->
            val decoded = json.parseToJsonElement(file.readText())

            val types: TypesMap = decoded.decodeMap("types")
            val functions: FunctionsMap = decoded.decodeMap("functions")
            val resources: ResourcesMap = decoded.decodeMap("resources")

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

        logger.info(json.encodeToString(stats))
        logger.info(json.encodeToString(moreStats))
    }
}

@Serializable
data class Example(val value: String, val from: String)

@Serializable
data class CountWithExamples(val count: Int = 0, val examples: List<Example> = emptyList()) {
    fun withOneMore(example: Example): CountWithExamples {
        return copy(
            count = count + 1,
            examples = (examples + example).take(3),
        )
    }
}

private inline fun <reified K, reified V> JsonElement.decodeMap(key: String): Map<K, V> =
    jsonObject[key]
        ?.let { Json.decodeFromJsonElement<Map<K, V>>(it) }
        .orEmpty()

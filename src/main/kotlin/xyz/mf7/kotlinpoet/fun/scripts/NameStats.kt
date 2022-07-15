package xyz.mf7.kotlinpoet.`fun`.scripts

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import xyz.mf7.kotlinpoet.`fun`.FunctionsMap
import xyz.mf7.kotlinpoet.`fun`.ResourcesMap
import xyz.mf7.kotlinpoet.`fun`.TypeMap
import java.io.File

data class CountWithExamples(val count: Int = 0, val examples: List<String> = emptyList()) {
    fun withOneMore(example: String): CountWithExamples {
        return copy(
            count = count + 1,
            examples = (examples + example).take(3)
        )
    }
}

fun main() {
    val files = listOf(
        "/Users/mfudala/workspace/kotlin-poet-fun/src/main/resources/schema.json",
        "/Users/mfudala/workspace/kotlin-poet-fun/src/main/resources/schema-aws-classic.json"
    )

    val stats = mutableMapOf<String, CountWithExamples>().withDefault { _ ->
        CountWithExamples()
    }

    val json = Json
    files.forEach {file ->
        val decoded = json.parseToJsonElement(File(file).readText())


        val types = decoded.decodeSection<TypeMap>("types")
        val functions = decoded.decodeSection<FunctionsMap>("functions")
        val resources = decoded.decodeSection<ResourcesMap>("resources")

        fun extractPattern(string: String): String {
            return string.replace(Regex("[^:/]"), "")
        }

        val names = resources.map { it.key } + types.map { it.key } + functions.map { it.key }
        names.forEach {name ->
            stats[extractPattern(name)] = stats[extractPattern(name)]?.withOneMore(name) ?: CountWithExamples()
        }

        println(names)
    }
}


inline fun <reified T> JsonElement.decodeSection(section: String) =
    Json.decodeFromJsonElement<T>(this.jsonObject["types"]!!)

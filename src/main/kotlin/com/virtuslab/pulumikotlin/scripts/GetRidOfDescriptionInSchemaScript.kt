package com.virtuslab.pulumikotlin.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

fun main(args: Array<String>) {
    GetRidOfDescriptionInSchemaScript().main(args)
}


class GetRidOfDescriptionInSchemaScript: CliktCommand() {
    private val schemaPath: String by option().required()

    override fun run() {
        val json = Json {
            prettyPrint = true
        }

        val inputSchema = File(schemaPath)

        val filesToProcess = if(inputSchema.isDirectory) {
            inputSchema.listFiles()?.toList().orEmpty()
        } else {
            listOf(inputSchema)
        }

        val modifiedNameSuffix = "-no-description"

        val filesNotAlreadyProcessed = filesToProcess
            .filterNot {
                it.name.contains(modifiedNameSuffix)
            }

        filesNotAlreadyProcessed.forEach { file ->
            val jsonContents = json.parseToJsonElement(
                file.bufferedReader().readText()
            )

            val withoutDescription = json.encodeToString(deleteDescription(jsonContents))
            val newPath = pathWithFileSuffix(file.path, suffix = modifiedNameSuffix)

            newPath.toFile().writeText(withoutDescription)
        }
    }

    private fun pathWithFileSuffix(path: String, suffix: String): Path {
        val originalPath = Path(path)

        val name = originalPath.nameWithoutExtension
        val extension = originalPath.extension
        return originalPath.parent.resolve("$name${suffix}.${extension}")
    }

    private fun deleteDescription(jsonElement: JsonElement): JsonElement {
        return when (jsonElement) {
            is JsonObject ->
                JsonObject(
                    jsonElement
                        .filterNot { (key, value) -> key == "description" }
                        .map { (key, value) -> key to deleteDescription(value) }
                        .toMap()
                )
            is JsonArray -> JsonArray(jsonElement.map { deleteDescription(it) })
            else -> jsonElement
        }
    }
}

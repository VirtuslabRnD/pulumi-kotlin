package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.InputStream

object Decoder {
    fun decode(inputStream: InputStream): ParsedSchema {
        val schema = Json.parseToJsonElement(inputStream.bufferedReader().readText())

        val types = schema.decodeOne<String, SchemaModel.RootTypeProperty>("types")
        val functions = schema.decodeOne<String, SchemaModel.Function>("functions")
        val resources = schema.decodeOne<String, SchemaModel.Resource>("resources")

        return ParsedSchema(types, functions, resources)
    }

    private inline fun <reified K, reified V> JsonElement.decodeOne(key: String): Map<K, V> =
        jsonObject[key]
            ?.let { Json.decodeFromJsonElement<Map<K, V>>(it) }
            .orEmpty()
}

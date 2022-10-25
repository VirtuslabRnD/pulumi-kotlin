package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.InputStream

object Decoder {
    fun decode(inputStream: InputStream): ParsedSchema {
        val schema = Json.parseToJsonElement(inputStream.bufferedReader().readText())

        val providerName = Json.decodeFromJsonElement<String>(schema.jsonObject["name"]!!) // FIXME !!
        val types = schema.decodeMap<String, SchemaModel.RootTypeProperty>("types")
        val functions = schema.decodeMap<String, SchemaModel.Function>("functions")
        val resources = schema.decodeMap<String, SchemaModel.Resource>("resources")
        val metadata = schema.decodeObject<SchemaModel.Metadata>("meta")
        val packageLanguage = schema.decodeObject<SchemaModel.PackageLanguage>("language")

        return ParsedSchema(providerName, types, functions, resources, metadata, packageLanguage)
    }

    private inline fun <reified K, reified V> JsonElement.decodeMap(key: String): Map<K, V> =
        jsonObject[key]
            ?.let { Json.decodeFromJsonElement<Map<K, V>>(it) }
            .orEmpty()

    private inline fun <reified T> JsonElement.decodeObject(propertyName: String): T? =
        jsonObject[propertyName]?.let { Json.decodeFromJsonElement<T>(it) }
}

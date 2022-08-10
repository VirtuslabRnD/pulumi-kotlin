package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.InputStream

object Decoder {
    fun decode(inputStream: InputStream): ParsedSchema {
        val schema = Json.parseToJsonElement(inputStream.bufferedReader().readText())

        val types = Json.decodeFromJsonElement<TypesMap>(schema.jsonObject["types"]!!)
        val functions = Json.decodeFromJsonElement<FunctionsMap>(schema.jsonObject["functions"]!!)
        val resources = Json.decodeFromJsonElement<ResourcesMap>(schema.jsonObject["resources"]!!)

        return ParsedSchema(types, functions, resources)
    }
}
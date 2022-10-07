package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.InputStream

object Decoder {
    fun decode(inputStream: InputStream): ParsedSchema {
        val schema = Json.parseToJsonElement(inputStream.bufferedReader().readText())

        val types = schema.jsonObject["types"]
            ?.let { Json.decodeFromJsonElement<TypesMap>(it) }
            ?: emptyMap()
        val functions = schema.jsonObject["functions"]
            ?.let { Json.decodeFromJsonElement<FunctionsMap>(it) }
            ?: emptyMap()
        val resources = schema.jsonObject["resources"]
            ?.let { Json.decodeFromJsonElement<ResourcesMap>(it) }
            ?: emptyMap()

        return ParsedSchema(types, functions, resources)
    }
}

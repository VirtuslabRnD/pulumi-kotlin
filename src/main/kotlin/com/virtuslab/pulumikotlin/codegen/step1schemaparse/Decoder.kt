package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Metadata
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PackageLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.InputStream

object Decoder {
    private val KNOWN_TYPE_DUPLICATES = setOf("azure-native:network:IpAllocationMethod")

    fun decode(inputStream: InputStream): ParsedSchema {
        val schema = Json.parseToJsonElement(inputStream.bufferedReader().readText())

        val providerName = requireNotNull(schema.decodeObject<String>("name")) {
            "Property \"name\" is not present in schema."
        }
        val types: TypesMap = schema.decodeMap("types")
        val functions: FunctionsMap = schema.decodeMap("functions")
        val resources: ResourcesMap = schema.decodeMap("resources")
        val metadata: Metadata? = schema.decodeObject("meta")
        val packageLanguage: PackageLanguage? = schema.decodeObject("language")

        return ParsedSchema(
            providerName,
            withoutKnownTypeDuplicates(types),
            functions,
            resources,
            metadata,
            packageLanguage,
        )
    }

    private inline fun <reified K, reified V> JsonElement.decodeMap(key: String): Map<K, V> =
        jsonObject[key]
            ?.let { Json.decodeFromJsonElement<Map<K, V>>(it) }
            .orEmpty()

    private inline fun <reified T> JsonElement.decodeObject(propertyName: String): T? =
        jsonObject[propertyName]?.let { Json.decodeFromJsonElement<T>(it) }

    private fun withoutKnownTypeDuplicates(types: TypesMap) = types.filterKeys { !KNOWN_TYPE_DUPLICATES.contains(it) }
}

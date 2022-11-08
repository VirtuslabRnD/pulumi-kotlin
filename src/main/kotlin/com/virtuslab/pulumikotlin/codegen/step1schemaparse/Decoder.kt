package com.virtuslab.pulumikotlin.codegen.step1schemaparse

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.RawFullProviderSchema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Schema
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream

object Decoder {
    private val KNOWN_TYPE_DUPLICATES = setOf("azure-native:network:IpAllocationMethod")

    fun decode(inputStream: InputStream): Schema {
        val rawFullProviderSchema =
            Json.decodeFromString<RawFullProviderSchema>(inputStream.bufferedReader().readText())

        return with(rawFullProviderSchema) {
            Schema(
                providerName = name,
                providerDisplayName = displayName,
                description = description,
                config = config,
                provider = provider,
                types = withoutKnownTypeDuplicates(types).filterValues { !it.isOverlay },
                functions = functions,
                resources = resources.filterValues { !it.isOverlay },
                metadata = meta,
                providerLanguage = language,
            )
        }
    }

    private fun withoutKnownTypeDuplicates(types: TypesMap) = types.filterKeys { !KNOWN_TYPE_DUPLICATES.contains(it) }
}

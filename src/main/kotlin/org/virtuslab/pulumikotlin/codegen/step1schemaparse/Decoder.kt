package org.virtuslab.pulumikotlin.codegen.step1schemaparse

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.RawFullProviderSchema
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Schema
import org.virtuslab.pulumikotlin.codegen.utils.Constants
import java.io.InputStream

object Decoder {

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

    private fun withoutKnownTypeDuplicates(types: TypesMap) =
        types.filterKeys { !Constants.DUPLICATED_TYPES.containsKey(it) }
}

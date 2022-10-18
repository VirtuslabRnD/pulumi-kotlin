import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Schema(val providerName: String, val url: String, val version: String, val customDependencies: List<String>)

fun getSchemas(versionConfig: File): List<Schema> {
    return Json.decodeFromString(
        ListSerializer(Schema.serializer()),
        versionConfig.readText(),
    )
}

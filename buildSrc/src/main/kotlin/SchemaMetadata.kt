import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

@Serializable
data class SchemaMetadata(
    val providerName: String,
    val url: String,
    val kotlinVersion: String,
    val javaVersion: String,
    val javaGitTag: String,
    val customDependencies: List<String>,
) {
    val versionedProvider: String
        get() = "$providerName${javaVersion.split(".").first()}"
}

fun SchemaMetadata.getKotlinGitTag() = "$providerName/v$kotlinVersion"

fun getSchemaMetadata(versionConfigFile: File): List<SchemaMetadata> {
    return Json.decodeFromString(
        versionConfigFile.readText(),
    )
}

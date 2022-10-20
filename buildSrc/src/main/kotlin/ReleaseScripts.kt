import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.maven.artifact.versioning.ComparableVersion
import java.io.File

@Suppress("OPT_IN_USAGE")
val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    prettyPrintIndent = "  "
}

@Serializable
data class VersionInfo(@SerialName("v") val version: String)

@Serializable
data class VersionInfoDetails(val docs: List<VersionInfo>)

@Serializable
class MavenSearchResponse(val response: VersionInfoDetails)

data class VersionStringPostfix(val id: String, val gitHash: String) {
    override fun toString(): String {
        return "$id+$gitHash"
    }
}

data class JavaVersion(val version: String, val postfix: VersionStringPostfix?) {

    companion object {
        fun fromVersionString(versionString: String): JavaVersion {
            val versionStringSegments = "(\\d+.\\d+.\\d+)(\\-(.*)\\+(.*))?".toRegex().find(versionString)
            val javaVersion = versionStringSegments?.groupValues?.get(1) ?: error("Invalid version string")
            val isRelease = versionStringSegments.groupValues[2].isEmpty()
            val postfix = if (!isRelease) {
                VersionStringPostfix(
                    versionStringSegments.groupValues[3],
                    versionStringSegments.groupValues[4],
                )
            } else {
                null
            }

            return JavaVersion(
                javaVersion,
                postfix,
            )
        }
    }

    override fun toString(): String {
        return "$version${if (postfix != null) "-$postfix" else ""}"
    }
}

data class KotlinVersion(val javaVersion: JavaVersion, val isSnapshot: Boolean, val minor: Int = 0) {

    companion object {
        fun fromVersionString(versionString: String): KotlinVersion {
            val versionStringSegments = "(\\d+.\\d+.\\d+).(\\d+)(\\-(.*)\\+([\\w\\d]*))?(\\-SNAPSHOT)?"
                .toRegex()
                .find(versionString)
            val javaVersion = versionStringSegments?.groupValues?.get(1) ?: error("Invalid version string")
            val isRelease = versionStringSegments.groupValues[3].isEmpty()
            val postfix = if (!isRelease) {
                VersionStringPostfix(
                    versionStringSegments.groupValues[4],
                    versionStringSegments.groupValues[5],
                )
            } else {
                null
            }
            val isSnapshot = versionStringSegments.groupValues[6].isNotEmpty()
            val kotlinMinor = versionStringSegments.groupValues[2]

            return KotlinVersion(
                JavaVersion(
                    javaVersion,
                    postfix,
                ),
                isSnapshot,
                kotlinMinor.toInt(),
            )
        }
    }

    override fun toString(): String {
        return "${javaVersion.version}.$minor${if (javaVersion.postfix != null) "-${javaVersion.postfix}" else ""}" +
            "${if (isSnapshot) "-SNAPSHOT" else ""}"
    }
}

fun updateGeneratorVersion(versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    val updatedSchemas = schemas.map {
        val oldKotlinVersion = KotlinVersion.fromVersionString(it.kotlinVersion)
        val newKotlinVersion = KotlinVersion(
            oldKotlinVersion.javaVersion,
            false,
            oldKotlinVersion.minor + 1,
        )

        SchemaMetadata(
            it.providerName,
            it.url,
            newKotlinVersion.toString(),
            newKotlinVersion.javaVersion.toString(),
            it.javaGitTag,
            it.customDependencies,
        )
    }

    versionConfigFile.writeText("${json.encodeToString(updatedSchemas)}\n")
}

fun updateProviderSchemas(versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    val updatedSchemas = fetchUpdatedSchemas(schemas, client)

    versionConfigFile.writeText("${json.encodeToString(updatedSchemas)}\n")

    client.close()
}

fun fetchUpdatedSchemas(
    schemas: List<SchemaMetadata>,
    client: HttpClient,
) = schemas.map {
    KotlinVersion
    val providerName = it.providerName
    val versions = fetchVersions(client, providerName, ComparableVersion(it.javaVersion))
    if (versions.isEmpty()) {
        it
    } else {
        val newJavaVersion = JavaVersion.fromVersionString(versions[0].toString())
        val newKotlinVersion = KotlinVersion(newJavaVersion, false)
        if (newJavaVersion.postfix != null) {
            println("git tag pulumi-$providerName/$newKotlinVersion")
        }
        val newGitTag = newJavaVersion.postfix?.gitHash ?: "v${newJavaVersion.version}"
        SchemaMetadata(
            providerName,
            it.url.replace(it.javaGitTag, newGitTag),
            newKotlinVersion.toString(),
            newJavaVersion.toString(),
            newGitTag,
            listOf("com.pulumi:$providerName:$newJavaVersion"),
        )
    }
}

fun updateVersionsAfterRelease(versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    val updatedSchemas = schemas.map {
        val oldKotlinVersion = KotlinVersion.fromVersionString(it.kotlinVersion)
        if (oldKotlinVersion.isSnapshot) {
            it
        } else {
            val newKotlinVersion = KotlinVersion(
                oldKotlinVersion.javaVersion,
                true,
                oldKotlinVersion.minor + 1,
            )
            SchemaMetadata(
                it.providerName,
                it.url,
                newKotlinVersion.toString(),
                it.javaVersion,
                it.javaGitTag,
                it.customDependencies,
            )
        }
    }

    versionConfigFile.writeText("${json.encodeToString(updatedSchemas)}\n")
}

private fun fetchVersions(
    client: HttpClient,
    provider: String,
    since: ComparableVersion,
): List<ComparableVersion> = runBlocking {
    return@runBlocking client.get {
        url {
            host = "search.maven.org"
            appendPathSegments("solrsearch", "select")
            encodedParameters.append(
                "q",
                "g:com.pulumi+AND+a:$provider",
            )
            encodedParameters.append(
                "wt",
                "json",
            )
            encodedParameters.append(
                "core",
                "gav",
            )
            encodedParameters.append(
                "rows",
                "20",
            )
        }
    }
        .body<MavenSearchResponse>()
        .response
        .docs
        .map { ComparableVersion(it.version) }
        .sorted()
        .filter { it > since }
}

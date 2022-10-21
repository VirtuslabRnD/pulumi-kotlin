import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.maven.artifact.versioning.ComparableVersion
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

val logger = KtorSimpleLogger("release-management")

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

fun updateGeneratorVersion(gitDirectory: File, versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    validateIfReleaseIsPossible(schemas)

    val updatedSchemas = schemas.map {
        val oldKotlinVersion = KotlinVersion.fromVersionString(it.kotlinVersion)
        val newKotlinVersion = KotlinVersion(
            oldKotlinVersion.javaVersion,
            false,
            if (oldKotlinVersion.isSnapshot) oldKotlinVersion.minor else oldKotlinVersion.minor + 1,
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

    val tags = getTags(updatedSchemas)
    val commitMessage = "Prepare release\n\n${tags.joinToString("\n")}"
    commitChangesInFile(gitDirectory, versionConfigFile, commitMessage)
}

fun updateProviderSchemas(gitDirectory: File, versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    validateIfReleaseIsPossible(schemas)

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

    val tags = getTags(updatedSchemas)
    val commitMessage = "Prepare release\n\n${tags.joinToString("\n")}"
    commitChangesInFile(gitDirectory, versionConfigFile, commitMessage)

    client.close()
}

fun fetchUpdatedSchemas(
    schemas: List<SchemaMetadata>,
    client: HttpClient,
) = schemas.map { schema ->
    val providerName = schema.providerName
    val versions = fetchVersions(client, providerName, ComparableVersion(schema.javaVersion))
    versions.map {
        val newJavaVersion = JavaVersion.fromVersionString(it.toString())
        val newKotlinVersion = KotlinVersion(newJavaVersion, false)
        val newGitTag = newJavaVersion.postfix?.gitHash ?: "v${newJavaVersion.version}"
        val url = schema.url.replace(schema.javaGitTag, newGitTag)
        SchemaMetadata(
            providerName,
            url,
            newKotlinVersion.toString(),
            newJavaVersion.toString(),
            newGitTag,
            listOf("com.pulumi:$providerName:$newJavaVersion"),
        )
    }
        .firstOrNull { newSchema ->
            val schemaExists = verifyUrl(client, newSchema.url)
            if (!schemaExists) {
                logger.warn("Skipping release ${newSchema.getKotlinTag()} (cannot find url: ${newSchema.url}")
            }
            schemaExists
        }
        ?: schema
}

fun replaceReleasedVersionsWithSnapshots(gitDirectory: File, versionConfigFile: File) {
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

    commitChangesInFile(gitDirectory, versionConfigFile, "Prepare for next development phase")
}

fun tagRecentReleases(gitDirectory: File, versionConfigFile: File) {
    val tagsToCreate = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())
        .filterNot { KotlinVersion.fromVersionString(it.kotlinVersion).isSnapshot }
        .map { it.getKotlinTag() }

    val git = Git(
        FileRepositoryBuilder()
            .setGitDir(File("$gitDirectory/.git"))
            .build(),
    )

    tagsToCreate.forEach {
        git.tag()
            .setName(it)
            .setSigned(false)
            .call()
    }
}

private fun fetchVersions(
    client: HttpClient,
    provider: String,
    since: ComparableVersion,
): List<ComparableVersion> = runBlocking {
    return@runBlocking fetchAllPagesSince(client, provider, since)
        .filter { it > since }
        .sorted()
}

private suspend fun fetchAllPagesSince(
    client: HttpClient,
    provider: String,
    since: ComparableVersion,
    pageNumber: Int = 0,
): List<ComparableVersion> {
    val fetchPage = fetchPage(client, provider, pageNumber)
    if (fetchPage.contains(since) || fetchPage.isEmpty()) {
        return fetchPage
    }
    return fetchPage + fetchAllPagesSince(client, provider, since, pageNumber + 1)
}

private suspend fun fetchPage(
    client: HttpClient,
    provider: String,
    pageNumber: Int,
    pageSize: Int = 20,
) = client.get {
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
            pageSize.toString(),
        )
        encodedParameters.append(
            "start",
            (pageSize * pageNumber).toString(),
        )
    }
}
    .body<MavenSearchResponse>()
    .response
    .docs
    .map { ComparableVersion(it.version) }

private fun verifyUrl(
    client: HttpClient,
    schemaUrl: String,
): Boolean = runBlocking {
    val schemaUrlObject = Url(schemaUrl)
    return@runBlocking client.head {
        url {
            protocol = schemaUrlObject.protocol
            host = schemaUrlObject.host
            pathSegments = schemaUrlObject.pathSegments
        }
    }
        .status
        .equals(HttpStatusCode.OK)
}

private fun commitChangesInFile(gitDirectory: File, relativePath: File, commitMessage: String) {
    val git = Git(
        FileRepositoryBuilder()
            .setGitDir(File("$gitDirectory/.git"))
            .build(),
    )
    git.add()
        .addFilepattern(relativePath.path)
        .call()
    git.commit()
        .setMessage(commitMessage)
        .setSign(false)
        .setAllowEmpty(false)
        .call()
}

private fun SchemaMetadata.getKotlinTag() = "$providerName/$kotlinVersion"

private fun getTags(updatedSchemas: List<SchemaMetadata>): List<String> {
    val tags = updatedSchemas.filterNot {
        KotlinVersion.fromVersionString(it.kotlinVersion).isSnapshot
    }
        .map { it.getKotlinTag() }
    return tags
}

private fun validateIfReleaseIsPossible(schemas: List<SchemaMetadata>) {
    if (
        schemas.map { it.kotlinVersion }
            .map { KotlinVersion.fromVersionString(it) }
            .filterNot { it.isSnapshot }
            .isNotEmpty()
    ) {
        error("Trying to create a release, but configuration already contains non-SNAPSHOT versions")
    }
}

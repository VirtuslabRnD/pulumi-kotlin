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
import org.semver4j.Semver
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

private fun Semver.getGitTag(): String = if (build.isNotEmpty()) build.joinToString(".") else "v$version"

data class KotlinVersion(val javaVersion: Semver, val kotlinMinor: Int = 0, val isSnapshot: Boolean) {
    companion object {
        fun fromVersionString(versionString: String): KotlinVersion {
            val versionStringSegments = "^(\\d+.\\d+.\\d+).(\\d+)(\\-.*\\+[\\w\\d]+)?(\\-SNAPSHOT)?\$"
                .toRegex()
                .find(versionString)
            val javaVersion = versionStringSegments?.groupValues?.get(1) ?: error("Invalid version string")
            val kotlinMinor = versionStringSegments.groupValues[2]
            val postfix = versionStringSegments.groupValues[3]
            val isSnapshot = versionStringSegments.groupValues[4].isNotEmpty()
            return KotlinVersion(
                Semver("$javaVersion$postfix"),
                kotlinMinor.toInt(),
                isSnapshot,
            )
        }
    }

    override fun toString(): String {
        val javaVersionWithoutPostfix = javaVersion.withClearedPreReleaseAndBuild().toString()

        return javaVersion
            .toString()
            .replace(
                javaVersionWithoutPostfix,
                "$javaVersionWithoutPostfix.$kotlinMinor",
            ) +
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
            oldKotlinVersion.kotlinMinor,
            false,
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
    val commitMessage = "Prepare release\n\n" +
        "This release includes the following versions:\n" +
        "${tags.joinToString("\n")}"
    commitChangesInFile(gitDirectory, versionConfigFile, commitMessage)

    client.close()
}

fun fetchUpdatedSchemas(
    schemas: List<SchemaMetadata>,
    client: HttpClient,
) = schemas.map { schema ->
    val providerName = schema.providerName
    val oldJavaVersion = Semver(schema.javaVersion)
    val versions = fetchVersions(
        client,
        providerName,
        ComparableVersion(schema.javaVersion),
        KotlinVersion.fromVersionString(schema.kotlinVersion),
    )
    versions.map {
        val newJavaVersion = Semver(it.toString())
        val newKotlinVersion = KotlinVersion(newJavaVersion, 0, false)
        val newGitTag = newJavaVersion.getGitTag()
        val newUrl = schema.url.replace(schema.javaGitTag, newGitTag)
        SchemaMetadata(
            providerName,
            newUrl,
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
            val newJavaVersion = Semver(newSchema.javaVersion)
            val sameMajor = newJavaVersion.major == oldJavaVersion.major
            if (!sameMajor) {
                logger.warn(
                    "Version with major update available: ${newSchema.getKotlinTag()}. " +
                        "Current version: ${schema.getKotlinTag()}",
                )
            }
            schemaExists && sameMajor
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
                oldKotlinVersion.kotlinMinor + 1,
                true,
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

    val git = createGitInstance(gitDirectory)

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
    kotlinVersion: KotlinVersion,
): List<ComparableVersion> = runBlocking {
    return@runBlocking fetchAllPagesSince(client, provider, since)
        .filter { it > since || (kotlinVersion.isSnapshot && kotlinVersion.kotlinMinor == 0 && it == since) }
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
    val git = createGitInstance(gitDirectory)
    git.add()
        .addFilepattern(relativePath.path)
        .call()
    git.commit()
        .setMessage(commitMessage)
        .setSign(false)
        .setAllowEmpty(false)
        .call()
}

private fun SchemaMetadata.getKotlinTag() = "$providerName/v$kotlinVersion"

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

private fun createGitInstance(gitDirectory: File) = Git(
    FileRepositoryBuilder()
        .setGitDir(File(gitDirectory, ".git"))
        .build(),
)

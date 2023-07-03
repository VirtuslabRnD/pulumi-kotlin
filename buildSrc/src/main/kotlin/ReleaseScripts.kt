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
import kotlinx.html.a
import kotlinx.html.stream.appendHTML
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr
import kotlinx.html.unsafe
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

data class KotlinVersion(val javaVersion: Semver, val kotlinMinor: Int, val isSnapshot: Boolean) {
    companion object {
        fun fromVersionString(versionString: String): KotlinVersion {
            val versionStringSegments = "^(\\d+.\\d+.\\d+).(\\d+)(-.*\\+[\\w\\d]+)?(-SNAPSHOT)?\$"
                .toRegex()
                .find(versionString)
                ?.groupValues
                ?: error("Invalid version string")

            val javaVersion = versionStringSegments[1]
            val kotlinMinor = versionStringSegments[2]
            val postfix = versionStringSegments[3]
            val isSnapshot = versionStringSegments[4].isNotEmpty()
            return KotlinVersion(
                Semver("$javaVersion$postfix"),
                kotlinMinor.toInt(),
                isSnapshot,
            )
        }
    }

    fun nextSnapshot() =
        if (isSnapshot) {
            this
        } else {
            KotlinVersion(
                javaVersion,
                kotlinMinor + 1,
                isSnapshot = true,
            )
        }

    fun previousRelease() =
        if (isSnapshot) {
            KotlinVersion(
                javaVersion,
                kotlinMinor - 1,
                isSnapshot = false,
            )
        } else {
            this
        }

    override fun toString(): String {
        val javaVersionWithoutPostfix = javaVersion.withClearedPreReleaseAndBuild().toString()

        val kotlinVersionWithoutClassifier = javaVersion
            .toString()
            .replace(
                javaVersionWithoutPostfix,
                "$javaVersionWithoutPostfix.$kotlinMinor",
            )
        val classifier = "-SNAPSHOT".takeIf { isSnapshot }.orEmpty()

        return kotlinVersionWithoutClassifier + classifier
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
            isSnapshot = false,
        )

        it.copy(
            kotlinVersion = newKotlinVersion.toString(),
            javaVersion = newKotlinVersion.javaVersion.toString(),
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
        engine {
            requestTimeout = 0
        }
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
        tags.joinToString("\n")
    commitChangesInFile(gitDirectory, versionConfigFile, commitMessage)

    client.close()
}

private fun fetchUpdatedSchemas(schemas: List<SchemaMetadata>, client: HttpClient): List<SchemaMetadata> {
    return schemas.map { schema ->
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
            val newKotlinVersion = KotlinVersion(newJavaVersion, kotlinMinor = 0, isSnapshot = false)
            val newGitTag = newJavaVersion.getGitTag()
            val newUrl = schema.url.replace(schema.javaGitTag, newGitTag)
            SchemaMetadata(
                providerName,
                newUrl,
                newKotlinVersion.toString(),
                newJavaVersion.toString(),
                newGitTag,
                schema.customDependencies,
            )
        }
            .firstOrNull { newSchema ->
                val schemaExists = verifyUrl(client, newSchema.url)
                if (!schemaExists) {
                    logger.warn("Skipping release ${newSchema.getKotlinGitTag()} (cannot find url: ${newSchema.url}")
                }
                val newJavaVersion = Semver(newSchema.javaVersion)
                val sameMajor = newJavaVersion.major == oldJavaVersion.major
                if (!sameMajor) {
                    logger.warn(
                        "Version with major update available: ${newSchema.getKotlinGitTag()}. " +
                            "Current version: ${schema.getKotlinGitTag()}",
                    )
                }
                schemaExists && sameMajor
            }
            ?: schema
    }
}

fun replaceReleasedVersionsWithSnapshots(gitDirectory: File, versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    val updatedSchemas = schemas.map {
        val oldKotlinVersion = KotlinVersion.fromVersionString(it.kotlinVersion)
        it.copy(kotlinVersion = oldKotlinVersion.nextSnapshot().toString())
    }

    versionConfigFile.writeText("${json.encodeToString(updatedSchemas)}\n")

    commitChangesInFile(gitDirectory, versionConfigFile, "Prepare for next development phase")
}

fun generateLatestVersionsMarkdownTable(versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    System.out.appendHTML().table {
        tr {
            th { +"Name" }
            th { +"Version" }
            th { +"Maven artifact" }
            th { +"Gradle artifact" }
            th { +"Maven Central" }
            th { +"Pulumi official docs" }
            th { +"Kotlin API docs" }
        }
        schemas.forEach {
            val providerName = it.providerName
            tr {
                val previousReleaseVersion = KotlinVersion.fromVersionString(it.kotlinVersion)
                    .previousRelease()
                    .toString()
                val mavenArtifactBlock = """
                       | 
                       | 
                       |```xml
                       |<dependency>
                       |     <groupId>org.virtuslab</groupId>
                       |     <artifactId>pulumi-$providerName-kotlin</artifactId>
                       |     <version>$previousReleaseVersion</version>
                       |</dependency>
                       |```
                       | 
                       | 
                """.trimMargin()
                val gradleArtifactBlock = """
                       | 
                       | 
                       |```kt
                       |implementation("org.virtuslab:pulumi-$providerName-kotlin:$previousReleaseVersion")
                       |```
                       | 
                       | 
                """.trimMargin()
                val githubPackagesUrl = "https://search.maven.org/artifact/org.virtuslab/pulumi-$providerName-kotlin"
                val pulumiOfficialDocsUrl = "https://www.pulumi.com/registry/packages/$providerName"
                val kotlinKDocUrl = "https://storage.googleapis.com/pulumi-kotlin-docs/$providerName/$previousReleaseVersion/index.html"

                td {
                    +providerName
                }
                td {
                    +previousReleaseVersion
                }
                td {
                    unsafe {
                        +mavenArtifactBlock
                    }
                }
                td {
                    unsafe {
                        +gradleArtifactBlock
                    }
                }
                td {
                    a(githubPackagesUrl) {
                        +"link"
                    }
                }
                td {
                    a(pulumiOfficialDocsUrl) {
                        +"link"
                    }
                }
                td {
                    a(kotlinKDocUrl) {
                        +"link"
                    }
                }
            }
        }
    }
}

fun tagRecentReleases(gitDirectory: File, versionConfigFile: File) {
    val tagsToCreate = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())
        .filterNot { KotlinVersion.fromVersionString(it.kotlinVersion).isSnapshot }
        .map { it.getKotlinGitTag() }

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
    fetchAllPagesSince(client, provider, since)
        .filter { it > since || (kotlinVersion.isSnapshot && kotlinVersion.kotlinMinor == 0 && it == since) }
        .sorted()
}

private fun fetchAllPagesSince(
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

private fun fetchPage(
    client: HttpClient,
    provider: String,
    pageNumber: Int,
    pageSize: Int = 20,
) = runBlocking {
    client.get {
        url {
            host = "search.maven.org"
            appendPathSegments("solrsearch", "select")
            with(encodedParameters) {
                append("q", "g:com.pulumi+AND+a:$provider")
                append("wt", "json")
                append("core", "gav")
                append("rows", pageSize.toString())
                append("start", (pageSize * pageNumber).toString())
            }
        }
    }
        .body<MavenSearchResponse>()
        .response
        .docs
        .map { ComparableVersion(it.version) }
}

private fun verifyUrl(
    client: HttpClient,
    schemaUrl: String,
): Boolean = runBlocking {
    client.head(Url(schemaUrl)).status == HttpStatusCode.OK
}

private fun commitChangesInFile(gitDirectory: File, absoluteFilePath: File, commitMessage: String) {
    val git = createGitInstance(gitDirectory)
    val relativePath = gitDirectory.toPath().relativize(absoluteFilePath.toPath()).toString()

    git.commit()
        .setOnly(relativePath)
        .setMessage(commitMessage)
        .setSign(false)
        .setAllowEmpty(false)
        .call()
}

private fun getTags(updatedSchemas: List<SchemaMetadata>): List<String> =
    updatedSchemas.filterNot {
        KotlinVersion.fromVersionString(it.kotlinVersion).isSnapshot
    }
        .map { it.getKotlinGitTag() }

private fun validateIfReleaseIsPossible(schemas: List<SchemaMetadata>) {
    val nonSnapshotVersions = schemas.map { it.kotlinVersion }
        .map { KotlinVersion.fromVersionString(it) }
        .filterNot { it.isSnapshot }
    if (nonSnapshotVersions.isNotEmpty()) {
        error("Trying to create a release, but configuration already contains non-SNAPSHOT versions")
    }
}

private fun createGitInstance(gitDirectory: File) = Git(
    FileRepositoryBuilder()
        .setGitDir(File(gitDirectory, ".git"))
        .build(),
)

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
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
import kotlinx.html.id
import kotlinx.html.stream.createHTML
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr
import kotlinx.html.unsafe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.maven.artifact.versioning.ComparableVersion
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.semver4j.Semver
import java.io.File
import java.util.concurrent.TimeUnit

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
            val versionStringSegments = """^(\d+\.\d+\.\d+)\.(\d+)(-.*?)??(-SNAPSHOT)?$"""
                .toRegex()
                .find(versionString)
                ?.groupValues
                ?: error("Invalid version string '$versionString'")

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

fun updateGeneratorVersion(gitDirectory: File, versionConfigFile: File, readmeFile: File) {
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

    updateReadme(readmeFile, versionConfigFile)

    val tags = getTags(updatedSchemas)
    val commitMessage = "Prepare release\n\n${tags.joinToString("\n")}"
    commitChangesInFiles(gitDirectory, commitMessage, versionConfigFile, readmeFile)
}

fun updateProviderSchemas(
    gitDirectory: File,
    versionConfigFile: File,
    readmeFile: File,
    skipPreReleaseVersions: Boolean,
    fastForwardToMostRecentVersion: Boolean,
    minimumNumberOfUpdates: Int,
) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    validateIfReleaseIsPossible(schemas)

    val client = HttpClient(CIO) {
        engine {
            requestTimeout = TimeUnit.MINUTES.toMillis(1)
        }
        install(HttpRequestRetry) {
            maxRetries = 5
            exponentialDelay()
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    val updatedSchemas = fetchUpdatedSchemas(schemas, client, skipPreReleaseVersions, fastForwardToMostRecentVersion)

    val numberOfUpdates = updatedSchemas.filter { !schemas.contains(it) }.size

    if (numberOfUpdates < minimumNumberOfUpdates) {
        logger.info(
            "The number of updates is insufficient for a commit " +
                "(minimum: $minimumNumberOfUpdates, actual: $numberOfUpdates)",
        )
        return
    }

    versionConfigFile.writeText("${json.encodeToString(updatedSchemas)}\n")

    updateReadme(readmeFile, versionConfigFile)

    val tags = getTags(updatedSchemas)
    val commitMessage = "Prepare release\n\n" +
        "This release includes the following versions:\n" +
        tags.joinToString("\n")
    commitChangesInFiles(gitDirectory, commitMessage, versionConfigFile, readmeFile)

    client.close()
}

private fun fetchUpdatedSchemas(
    schemas: List<SchemaMetadata>,
    client: HttpClient,
    skipPreReleaseVersions: Boolean,
    fastForwardToMostRecentVersion: Boolean,
): List<SchemaMetadata> {
    return schemas.map { oldSchema ->
        val providerName = oldSchema.providerName
        val oldJavaVersion = Semver(oldSchema.javaVersion)
        val versions = fetchVersions(
            client,
            providerName,
            ComparableVersion(oldSchema.javaVersion),
            KotlinVersion.fromVersionString(oldSchema.kotlinVersion),
            skipPreReleaseVersions,
        )
            .map {
                val newKotlinVersion = KotlinVersion(it, kotlinMinor = 0, isSnapshot = false)
                val newGitTag = it.getGitTag()
                val newUrl = oldSchema.url.replace(oldSchema.javaGitTag, newGitTag)
                SchemaMetadata(
                    providerName,
                    newUrl,
                    newKotlinVersion.toString(),
                    it.toString(),
                    newGitTag,
                    oldSchema.customDependencies,
                )
            }

        val otherVersionsOfProvider = schemas.filter {
            it.providerName == providerName && it.kotlinVersion != oldSchema.kotlinVersion
        }
            .map { Semver(it.javaVersion) }

        val newSchema = if (fastForwardToMostRecentVersion) {
            versions.lastOrNull { validateVersion(client, it, oldSchema, oldJavaVersion, otherVersionsOfProvider) }
        } else {
            versions.firstOrNull { validateVersion(client, it, oldSchema, oldJavaVersion, otherVersionsOfProvider) }
        }

        newSchema ?: oldSchema
    }
}

private fun updateReadme(readmeFile: File, versionConfigFile: File) {
    val readme = readmeFile.readText()
    val newTable = getLatestVersionsMarkdownTable(versionConfigFile)
    val newReadme = "<table id=\"pulumi-kotlin-versions-table\">.*?</table>\n"
        .toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        .replace(readme, newTable)

    readmeFile.writeText(newReadme)
}

fun replaceReleasedVersionsWithSnapshots(gitDirectory: File, versionConfigFile: File) {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())

    val updatedSchemas = schemas.map {
        val oldKotlinVersion = KotlinVersion.fromVersionString(it.kotlinVersion)
        it.copy(kotlinVersion = oldKotlinVersion.nextSnapshot().toString())
    }

    versionConfigFile.writeText("${json.encodeToString(updatedSchemas)}\n")

    commitChangesInFiles(gitDirectory, "Prepare for next development phase", versionConfigFile)
}

fun getLatestVersionsMarkdownTable(versionConfigFile: File): String {
    val schemas = Json.decodeFromString<List<SchemaMetadata>>(versionConfigFile.readText())
    return createHTML().table {
        id = "pulumi-kotlin-versions-table"
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
                val kotlinKDocUrl =
                    "https://storage.googleapis.com/pulumi-kotlin-docs/$providerName/$previousReleaseVersion/index.html"

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
    skipPreReleaseVersions: Boolean,
): List<Semver> = runBlocking {
    fetchAllPagesSince(client, provider, since)
        .asSequence()
        .filter { it > since || (kotlinVersion.isSnapshot && kotlinVersion.kotlinMinor == 0 && it == since) }
        .map { Semver(it.toString()) }
        .sorted()
        .filter { if (skipPreReleaseVersions) it.preRelease.isEmpty() else true }
        .toList()
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

private fun validateVersion(
    client: HttpClient,
    newSchema: SchemaMetadata,
    oldSchema: SchemaMetadata,
    oldJavaVersion: Semver,
    otherVersionsOfProvider: List<Semver>,
): Boolean {
    val newJavaVersion = Semver(newSchema.javaVersion)
    val sameMajor = newJavaVersion.major == oldJavaVersion.major
    val majorAlreadyInVersionConfig = otherVersionsOfProvider.any { it.major == newJavaVersion.major }
    if (!sameMajor && !majorAlreadyInVersionConfig) {
        logger.warn(
            "Version with major update available: ${newSchema.getKotlinGitTag()}. " +
                "Current version: ${oldSchema.getKotlinGitTag()}",
        )
    }
    val schemaExists = verifyUrl(client, newSchema.url)
    if (sameMajor && !schemaExists) {
        logger.warn("Skipping release ${newSchema.getKotlinGitTag()} (cannot find url: ${newSchema.url})")
    }
    return schemaExists && sameMajor
}

private fun commitChangesInFiles(gitDirectory: File, commitMessage: String, vararg absoluteFilePaths: File) {
    val git = createGitInstance(gitDirectory)

    val commit = absoluteFilePaths
        .fold(git.commit()) { commit, file ->
            val relativePath = gitDirectory.toPath().relativize(file.toPath()).toString()
            commit.setOnly(relativePath)
        }

    commit
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

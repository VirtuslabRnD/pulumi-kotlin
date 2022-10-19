import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals

class ReleaseScriptsTest {

    @Test
    fun `correctly parses Kotlin library version (release)`() {
        val versionString = "5.16.0.2"
        val kotlinVersion = KotlinVersion.fromVersionString(versionString)

        val expectedParsedVersion = KotlinVersion(
            JavaVersion(
                "5.16.0",
                null,
            ),
            2,
        )

        assertEquals(
            expectedParsedVersion,
            kotlinVersion,
        )
        assertEquals(
            versionString,
            kotlinVersion.toString()
        )
    }

    @Test
    fun `correctly parses Kotlin library version (non-release)`() {
        val versionString = "4.7.0.2-alpha.1657304919+1d411918"
        val kotlinVersion = KotlinVersion.fromVersionString(versionString)

        val expectedParsedVersion = KotlinVersion(
            JavaVersion(
                "4.7.0",
                VersionStringPostfix(
                    "alpha.1657304919",
                    "1d411918",
                ),
            ),
            2,
        )

        assertEquals(
            expectedParsedVersion,
            kotlinVersion,
        )
        assertEquals(
            versionString,
            kotlinVersion.toString()
        )
    }

    @Test
    fun `correctly parses Java library version (release)`() {
        val versionString = "5.16.0"
        val javaVersion = JavaVersion.fromVersionString(versionString)

        val expectedParsedVersion = JavaVersion(
            "5.16.0",
            null,
        )

        assertEquals(
            expectedParsedVersion,
            javaVersion,
        )
        assertEquals(
            versionString,
            javaVersion.toString()
        )
    }

    @Test
    fun `correctly parses Java library version (non-release)`() {
        val versionString = "4.7.0-alpha.1657304919+1d411918"
        val javaVersion = JavaVersion.fromVersionString(versionString)

        val expectedParsedVersion = JavaVersion(
            "4.7.0",
            VersionStringPostfix(
                "alpha.1657304919",
                "1d411918",
            ),
        )

        assertEquals(
            expectedParsedVersion,
            javaVersion,
        )
        assertEquals(
            versionString,
            javaVersion.toString()
        )
    }

    @Test
    fun `fetches new schema versions`() {
        val schemasBeforeUpdate = Json.decodeFromString<List<SchemaMetadata>>(
            File("src/test/resources/before-provider-updates.json").readText(),
        )
        val schemasAfterUpdate = Json.decodeFromString<List<SchemaMetadata>>(
            File("src/test/resources/after-provider-updates.json").readText(),
        )

        val responses = mapOf(
            "slack" to createMavenSearchResponse(
                "0.2.2-alpha.1660927837+572a130c",
                "0.2.2",
                "0.2.3-alpha.1661880655+57fde9d4",
                "0.2.3-alpha.1662678388+4c0a6446",
                "0.3.0",
                "0.3.1-alpha.1663343588+9977be98",
            ),
            "random" to createMavenSearchResponse(
                "4.6.0",
                "4.7.0-alpha.1657304919+1d411918",
                "4.7.0-alpha.1657724819+25d10298",
                "4.7.0",
                "4.8.1",
            ),
            "aws" to createMavenSearchResponse(),
            "gcp" to createMavenSearchResponse(
                "6.40.0",
                "6.40.0-alpha.1664404181+3cd0302a",
                "6.39.0",
                "6.38.0-alpha.1663880792+639e360f",
            ),
        )

        val updatedSchemas = fetchUpdatedSchemas(schemasBeforeUpdate, createMockHttpClient(responses))

        assertEquals(
            schemasAfterUpdate,
            updatedSchemas,
        )
    }

    private fun createMavenSearchResponse(vararg docs: String) =
        MavenSearchResponse(
            VersionInfoDetails(
                docs.map { VersionInfo(it) },
            ),
        )

    @Test
    fun `updates versions after generator update`() {
        val updatedFileLocation = File("build/tmp/before-generator-update-${RandomStringUtils.random(10)}.json")
        val expectedResultsFile = File("src/test/resources/after-generator-update.json")

        Files.copy(
            File("src/test/resources/before-generator-update.json").toPath(),
            updatedFileLocation.toPath(),
        )

        updateGeneratorVersion(updatedFileLocation)

        assertEquals(
            updatedFileLocation.readText(),
            expectedResultsFile.readText(),
        )
    }

    private fun createMockHttpClient(responses: Map<String, MavenSearchResponse>) =
        HttpClient(
            MockEngine { request ->
                val response = responses.filter {
                    request.url.parameters["q"]?.contains(it.key) ?: false
                }
                    .values
                    .firstOrNull()
                    ?: createMavenSearchResponse()

                val value = ContentType.Application.Json.toString()
                respond(
                    content = Json.encodeToString(response),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, value),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
        }
}

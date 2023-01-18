import org.apache.commons.lang3.RandomStringUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.semver4j.Semver
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val RESOURCES = "src/test/resources"

class ReleaseScriptsTest {

    @Test
    fun `correctly parses Kotlin library version (Java release, Kotlin release)`() {
        val versionString = "5.16.0.2"
        val kotlinVersion = KotlinVersion.fromVersionString(versionString)

        val expectedParsedVersion = KotlinVersion(Semver("5.16.0"), 2, false)

        assertEquals(
            expectedParsedVersion,
            kotlinVersion,
        )
        assertEquals(
            versionString,
            kotlinVersion.toString(),
        )
    }

    @Test
    fun `correctly parses Kotlin library version (Java release, Kotlin SNAPSHOT)`() {
        val versionString = "5.16.0.2-SNAPSHOT"
        val kotlinVersion = KotlinVersion.fromVersionString(versionString)

        val expectedParsedVersion = KotlinVersion(Semver("5.16.0"), 2, true)

        assertEquals(
            expectedParsedVersion,
            kotlinVersion,
        )
        assertEquals(
            versionString,
            kotlinVersion.toString(),
        )
    }

    @Test
    fun `correctly parses Kotlin library version (Java alpha, Kotlin alpha)`() {
        val versionString = "4.7.0.2-alpha.1657304919+1d411918"
        val kotlinVersion = KotlinVersion.fromVersionString(versionString)

        val expectedParsedVersion = KotlinVersion(Semver("4.7.0-alpha.1657304919+1d411918"), 2, false)

        assertEquals(
            expectedParsedVersion,
            kotlinVersion,
        )
        assertEquals(
            versionString,
            kotlinVersion.toString(),
        )
    }

    @Test
    fun `correctly parses Kotlin library version (Java alpha, Kotlin alpha SNAPSHOT)`() {
        val versionString = "4.7.0.2-alpha.1657304919+1d411918-SNAPSHOT"
        val kotlinVersion = KotlinVersion.fromVersionString(versionString)

        val expectedParsedVersion = KotlinVersion(Semver("4.7.0-alpha.1657304919+1d411918"), 2, true)

        assertEquals(
            expectedParsedVersion,
            kotlinVersion,
        )
        assertEquals(
            versionString,
            kotlinVersion.toString(),
        )
    }

    @Test
    fun `fails to parses Kotlin library version if no Git hash is available in a pre-release`() {
        val versionString = "4.7.0.2-alpha.1657304919"

        assertFailsWith<IllegalStateException>("Invalid version string") {
            KotlinVersion.fromVersionString(versionString)
        }
    }

    @Test
    @Disabled("Disabled due to unreliable search.maven.org (https://github.com/VirtuslabRnD/pulumi-kotlin/issues/202)")
    fun `updates provider schema versions`() {
        val temporaryGitRepository = File("build/tmp/provider-update-test-${RandomStringUtils.randomAlphanumeric(10)}")
        val beforeUpdateFileName = "before-schema-update.json"
        val afterUpdateFileName = "after-schema-update.json"
        val temporaryBeforeUpdateFile = File("$temporaryGitRepository/$beforeUpdateFileName")
        val expectedAfterUpdateFile = File("$RESOURCES/$afterUpdateFileName")

        createRepoWithSingleFile(
            temporaryGitRepository,
            beforeUpdateFileName,
        )

        updateProviderSchemas(temporaryGitRepository, temporaryBeforeUpdateFile)

        assertEquals(
            expectedAfterUpdateFile.readText(),
            temporaryBeforeUpdateFile.readText(),
        )
    }

    @Test
    fun `updates versions after generator update`() {
        val temporaryGitRepository = File("build/tmp/generator-update-test-${RandomStringUtils.randomAlphanumeric(10)}")
        val beforeUpdateFileName = "before-generator-update.json"
        val afterUpdateFileName = "after-generator-update.json"
        val temporaryBeforeUpdateFile = File("$temporaryGitRepository/$beforeUpdateFileName")
        val expectedAfterUpdateFile = File("$RESOURCES/$afterUpdateFileName")

        createRepoWithSingleFile(
            temporaryGitRepository,
            beforeUpdateFileName,
        )

        updateGeneratorVersion(temporaryGitRepository, temporaryBeforeUpdateFile)

        assertEquals(
            expectedAfterUpdateFile.readText(),
            temporaryBeforeUpdateFile.readText(),
        )
    }

    @Test
    fun `cleans up after release (moves to SNAPSHOT versions)`() {
        val temporaryGitRepository = File(
            "build/tmp/release-clean-up-test-${RandomStringUtils.randomAlphanumeric(10)}",
        )
        val beforeUpdateFileName = "before-cleanup.json"
        val afterUpdateFileName = "after-cleanup.json"
        val temporaryBeforeUpdateFile = File("$temporaryGitRepository/$beforeUpdateFileName")
        val expectedAfterUpdateFile = File("$RESOURCES/$afterUpdateFileName")

        createRepoWithSingleFile(
            temporaryGitRepository,
            beforeUpdateFileName,
        )

        replaceReleasedVersionsWithSnapshots(temporaryGitRepository, temporaryBeforeUpdateFile)

        assertEquals(
            expectedAfterUpdateFile.readText(),
            temporaryBeforeUpdateFile.readText(),
        )
    }

    @Test
    fun `tags released versions`() {
        val temporaryGitRepository = File("build/tmp/tag-release-test-${RandomStringUtils.randomAlphanumeric(10)}")
        val versionConfigFileName = "after-schema-update.json"
        val versionConfigFile = File("$RESOURCES/$versionConfigFileName")
        val temporaryVersionConfigFile = File("$temporaryGitRepository/$versionConfigFileName")

        val git = createRepoWithSingleFile(
            temporaryGitRepository,
            versionConfigFileName,
        )

        tagRecentReleases(temporaryGitRepository, temporaryVersionConfigFile)

        assertEquals(
            versionConfigFile.readText(),
            temporaryVersionConfigFile.readText(),
        )

        val tagList = git.tagList()
            .call()
            .map { it.name }
            .map { it.replace("refs/tags/", "") }

        assert(
            tagList.contains("slack/v0.2.2.0") &&
                tagList.contains("random/v4.8.1.0") &&
                tagList.contains("aws/v5.17.0.0-alpha.1665590627+9c01b95f") &&
                tagList.contains("gcp/v6.39.0.0"),
        )
    }

    private fun createRepoWithSingleFile(
        temporaryGitRepository: File,
        beforeUpdateFileName: String,
        commitMessage: String = "Add version config",
    ): Git {
        val repository = FileRepositoryBuilder.create(File(temporaryGitRepository, ".git"))
        repository.create()

        Files.copy(
            File("$RESOURCES/$beforeUpdateFileName").toPath(),
            File("$temporaryGitRepository/$beforeUpdateFileName").toPath(),
        )

        val git = Git(repository)
        git.add().addFilepattern(beforeUpdateFileName).call()
        git.commit()
            .setMessage(commitMessage)
            .setSign(false)
            .setAllowEmpty(false)
            .call()

        return git
    }
}

import org.apache.commons.lang3.RandomStringUtils
import org.apache.maven.artifact.versioning.ComparableVersion
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals

class ReleaseScriptsTest {

    @Test
    fun `produces valid versioning model`() {
        assert(ComparableVersion("0.9.10.5") < ComparableVersion("1.0.0.0-SNAPSHOT"))
        assert(ComparableVersion("1.0.0.0-SNAPSHOT") < ComparableVersion("1.0.0.0"))
        assert(ComparableVersion("1.0.0.0") < ComparableVersion("1.0.0.1"))
        assert(ComparableVersion("1.0.0.2") < ComparableVersion("1.0.0.10"))

        assert(
            ComparableVersion("4.10.1.0-alpha.1665590627+9c01b95f") <
                ComparableVersion("5.17.0.10-alpha.1665590627+9c01b95f"),
        )
        assert(
            ComparableVersion("5.17.0.0-alpha.1665590627+9c01b95f-SNAPSHOT") <
                ComparableVersion("5.17.0.0-alpha.1665590627+9c01b95f"),
        )
        assert(
            ComparableVersion("5.17.0.0-alpha.1665590627+9c01b95f") <
                ComparableVersion("5.17.0.1-alpha.1665590627+9c01b95f"),
        )
        assert(
            ComparableVersion("5.17.0.2-alpha.1665590627+9c01b95f") <
                ComparableVersion("5.17.0.10-alpha.1665590627+9c01b95f"),
        )
        assert(
            ComparableVersion("5.14.0-alpha.1663282832+a2389a26") <
                ComparableVersion("5.14.0-alpha.1663343686+d0e52280"),
        )

        assert(ComparableVersion("0.9.10.5-alpha.1665590627+9c01b95f") < ComparableVersion("0.9.10.5"))
    }

    @Test
    fun `correctly parses Kotlin library version (Java release, Kotlin release)`() {
        val versionString = "5.16.0.2"
        val kotlinVersion = KotlinVersion.fromVersionString(versionString)

        val expectedParsedVersion = KotlinVersion(
            JavaVersion(
                "5.16.0",
                null,
            ),
            false,
            2,
        )

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

        val expectedParsedVersion = KotlinVersion(
            JavaVersion(
                "5.16.0",
                null,
            ),
            true,
            2,
        )

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

        val expectedParsedVersion = KotlinVersion(
            JavaVersion(
                "4.7.0",
                VersionStringPostfix(
                    "alpha.1657304919",
                    "1d411918",
                ),
            ),
            false,
            2,
        )

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

        val expectedParsedVersion = KotlinVersion(
            JavaVersion(
                "4.7.0",
                VersionStringPostfix(
                    "alpha.1657304919",
                    "1d411918",
                ),
            ),
            true,
            2,
        )

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
            javaVersion.toString(),
        )
    }

    @Test
    fun `correctly parses Java library version (alpha)`() {
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
            javaVersion.toString(),
        )
    }

    @Test
    fun `updates provider schema versions`() {
        val temporaryGitRepository = "build/tmp/provider-update-test-${RandomStringUtils.randomAlphanumeric(10)}"
        val beforeUpdateFileName = "before-schema-update.json"
        val afterUpdateFileName = "after-schema-update.json"

        FileRepositoryBuilder.create(File(temporaryGitRepository, ".git")).create()

        val temporaryBeforeUpdateFile = File("$temporaryGitRepository/$beforeUpdateFileName")
        val expectedAfterUpdateFile = File("src/test/resources/$afterUpdateFileName")

        Files.copy(
            File("src/test/resources/$beforeUpdateFileName").toPath(),
            temporaryBeforeUpdateFile.toPath(),
        )

        updateProviderSchemas(File(temporaryGitRepository), temporaryBeforeUpdateFile)

        assertEquals(
            expectedAfterUpdateFile.readText(),
            temporaryBeforeUpdateFile.readText(),
        )
    }

    @Test
    fun `updates versions after generator update`() {
        val temporaryGitRepository = "build/tmp/generator-update-test-${RandomStringUtils.randomAlphanumeric(10)}"
        val beforeUpdateFileName = "before-generator-update.json"
        val afterUpdateFileName = "after-generator-update.json"

        FileRepositoryBuilder.create(File(temporaryGitRepository, ".git")).create()

        val temporaryBeforeUpdateFile = File("$temporaryGitRepository/$beforeUpdateFileName")
        val expectedAfterUpdateFile = File("src/test/resources/$afterUpdateFileName")

        Files.copy(
            File("src/test/resources/$beforeUpdateFileName").toPath(),
            temporaryBeforeUpdateFile.toPath(),
        )

        updateGeneratorVersion(File(temporaryGitRepository), temporaryBeforeUpdateFile)

        assertEquals(
            expectedAfterUpdateFile.readText(),
            temporaryBeforeUpdateFile.readText(),
        )
    }

    @Test
    fun `cleans up after release (moves to SNAPSHOT versions)`() {
        val temporaryGitRepository = "build/tmp/release-clean-up-test-${RandomStringUtils.randomAlphanumeric(10)}"
        val beforeUpdateFileName = "before-cleanup.json"
        val afterUpdateFileName = "after-cleanup.json"

        FileRepositoryBuilder.create(File(temporaryGitRepository, ".git")).create()

        val temporaryBeforeUpdateFile = File("$temporaryGitRepository/$beforeUpdateFileName")
        val expectedAfterUpdateFile = File("src/test/resources/$afterUpdateFileName")

        Files.copy(
            File("src/test/resources/$beforeUpdateFileName").toPath(),
            temporaryBeforeUpdateFile.toPath(),
        )

        replaceReleasedVersionsWithSnapshots(File(temporaryGitRepository), temporaryBeforeUpdateFile)

        assertEquals(
            expectedAfterUpdateFile.readText(),
            temporaryBeforeUpdateFile.readText(),
        )
    }

    @Test
    fun `tags released versions`() {
        val temporaryGitRepository = "build/tmp/tag-release-test-${RandomStringUtils.randomAlphanumeric(10)}"
        val beforeUpdateFileName = "after-schema-update.json"

        val repository = FileRepositoryBuilder.create(File(temporaryGitRepository, ".git"))
        repository.create()
        val git = Git(repository)

        val beforeUpdateFile = File("src/test/resources/$beforeUpdateFileName")
        val temporaryBeforeUpdateFile = File("$temporaryGitRepository/$beforeUpdateFileName")

        Files.copy(
            beforeUpdateFile.toPath(),
            temporaryBeforeUpdateFile.toPath(),
        )

        git.add().addFilepattern(beforeUpdateFileName).call()
        git.commit()
            .setMessage("Prepare release")
            .setSign(false)
            .setAllowEmpty(false)
            .call()
        git.branchRename().setOldName("master").setNewName("main").call()

        tagRecentReleases(File(temporaryGitRepository), temporaryBeforeUpdateFile)

        assertEquals(
            beforeUpdateFile.readText(),
            temporaryBeforeUpdateFile.readText(),
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
}

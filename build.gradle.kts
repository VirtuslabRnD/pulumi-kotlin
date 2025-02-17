import org.gradle.configurationcache.extensions.capitalized

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jmailen.kotlinter")
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("code-generation")
}

group = "org.virtuslab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.virtuslab:pulumi-kotlin:0.11.0.0")

    implementation("com.squareup:kotlinpoet:1.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")

    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    // Maven Archeologist has two dependencies with vulnerabilities (com.squareup.okio:okio:2.6.0 and 
    // com.squareup.okhttp3:okhttp:4.7.2), which we chose to replace with newer versions.
    // See: https://nvd.nist.gov/vuln/detail/CVE-2023-3635
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.tools.build:maven-archeologist:0.0.10") {
        exclude("com.squareup.okio", "okio")
        exclude("com.squareup.okhttp3", "okhttp")
    }

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.6.0")
    testImplementation(kotlin("test", "1.9.22"))
    testImplementation("com.google.cloud:google-cloud-compute:1.62.0")
    testImplementation("com.azure:azure-identity:1.14.0")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager-compute:2.43.0")
    testImplementation("io.kubernetes:client-java:21.0.2")
    testImplementation("io.github.cdklabs:projen:0.88.5")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.virtuslab.pulumikotlin.codegen.MainKt")
}

tasks.withType<Jar> {
    archiveBaseName.set("${project.rootProject.name}-generator")
}

val versionConfigFile = File(projectDir, "src/main/resources/version-config.json")
val readmeFile = File(projectDir, "README.md")
var schemaMetadata: List<SchemaMetadata> = getSchemaMetadata(versionConfigFile)

val createTasksForProvider: (SchemaMetadata) -> Unit by extra
val createGlobalProviderTasks: (List<SchemaMetadata>) -> Unit by extra

schemaMetadata.forEach { createTasksForProvider(it) }
createGlobalProviderTasks(schemaMetadata)

val e2eVersionConfigFile = File(projectDir, "src/main/resources/version-config-e2e.json")
var e2eSchemaMetadata: List<SchemaMetadata> = getSchemaMetadata(e2eVersionConfigFile)
val createE2eTasksForProvider: (SchemaMetadata) -> Unit by extra

e2eSchemaMetadata.forEach { createE2eTasksForProvider(it) }

sourceSets {
    create("e2eTest") {
        java {
            srcDir("src/e2e/kotlin")
            compileClasspath += sourceSets["test"].compileClasspath
            runtimeClasspath += sourceSets["test"].runtimeClasspath
        }
    }
}

tasks.register<Test>("e2eTest") {
    group = "verification"
    testClassesDirs = sourceSets["e2eTest"].output.classesDirs
    classpath = sourceSets["e2eTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.register<JavaExec>("computeSchemaSubset") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.virtuslab.pulumikotlin.scripts.ComputeSchemaSubsetScriptKt")
}

kotlinter {
    reporters = arrayOf("html", "plain")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/.detekt-config.yml")
}

// By default, `detekt` doesn't include `detektMain` and `detektTest` and it only runs those checks that do not require
// type resolution. See: https://detekt.dev/docs/gettingstarted/type-resolution/
tasks["detekt"].dependsOn(tasks["detektMain"])
tasks["detekt"].dependsOn(tasks["detektTest"])
tasks["detekt"].dependsOn(tasks["detektE2eTest"])

tasks.register<Task>("prepareReleaseOfUpdatedSchemas") {
    group = "releaseManagement"
    val skipPreReleaseVersions = findBooleanProperty("skipPreReleaseVersions", defaultValue = false)
    val fastForwardToMostRecentVersion = findBooleanProperty("fastForwardToMostRecentVersion", defaultValue = false)
    val minimumNumberOfUpdates = findTypedProperty<String?>("minimumNumberOfUpdates")?.toInt() ?: 1

    doLast {
        updateProviderSchemas(
            gitDirectory = projectDir,
            versionConfigFile = versionConfigFile,
            readmeFile = readmeFile,
            skipPreReleaseVersions = skipPreReleaseVersions,
            fastForwardToMostRecentVersion = fastForwardToMostRecentVersion,
            minimumNumberOfUpdates = minimumNumberOfUpdates
        )
    }
}

tasks.register<Task>("prepareReleaseAfterGeneratorUpdate") {
    group = "releaseManagement"
    doLast {
        updateGeneratorVersion(
            gitDirectory = projectDir,
            versionConfigFile = versionConfigFile,
            readmeFile = readmeFile,
        )
    }
}

tasks.register<Task>("tagRecentRelease") {
    group = "releaseManagement"
    doLast {
        tagRecentReleases(projectDir, versionConfigFile)
    }
}

tasks.register<Task>("postRelease") {
    group = "releaseManagement"
    doLast {
        replaceReleasedVersionsWithSnapshots(projectDir, versionConfigFile)
    }
}

tasks.register<Task>("latestVersionsMarkdownTable") {
    group = "releaseManagement"
    doLast {
        println(getLatestVersionsMarkdownTable(versionConfigFile))
    }
}

publishing {
    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findTypedProperty("sonatype.username")
                password = findTypedProperty("sonatype.password")
            }
        }
    }
}

val publicationsToPublishToMavenCentral = schemaMetadata
    .filterNot { KotlinVersion.fromVersionString(it.kotlinVersion).isSnapshot }
    .map { "publishPulumi${it.versionedProviderName.capitalized()}PublicationToMavenCentralRepository" }

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        repository.name == "MavenCentral" && publicationsToPublishToMavenCentral.contains(it.name)
    }
}

val enableSigning = findTypedProperty<String>("signing.enabled").toBoolean()

if (enableSigning) {
    val signingKey: String = findTypedProperty("signing.key")
    val signingKeyPassword: String = findTypedProperty("signing.key.password")

    signing {
        useInMemoryPgpKeys(signingKey, signingKeyPassword)
    }
}

inline fun <reified T> findTypedProperty(propertyString: String): T = findProperty(propertyString) as T

fun findBooleanProperty(propertyName: String, defaultValue: Boolean) = findTypedProperty<String?>(propertyName)
    ?.toBoolean()
    ?: defaultValue

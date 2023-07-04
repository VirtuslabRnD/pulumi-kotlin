import org.gradle.configurationcache.extensions.capitalized

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.20"
    id("org.jmailen.kotlinter") version "3.14.0"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("code-generation")
}

group = "org.virtuslab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.pulumi:pulumi:0.9.4")

    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.2")

    implementation("com.github.ajalt.clikt:clikt:3.5.4")

    implementation("com.squareup.tools.build:maven-archeologist:0.0.10")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.8")

    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation(kotlin("test"))
    testImplementation("com.google.cloud:google-cloud-compute:1.30.0")
    testImplementation("com.azure:azure-identity:1.9.1")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager-compute:2.28.0")

    testImplementation("io.kubernetes:client-java:18.0.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("io.github.cdklabs:projen:0.71.120")
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
var schemaMetadata: List<SchemaMetadata> = getSchemaMetadata(versionConfigFile)

val createTasksForProvider: (SchemaMetadata) -> Unit by extra
val createGlobalProviderTasks: (List<String>) -> Unit by extra

schemaMetadata.forEach { createTasksForProvider(it) }
createGlobalProviderTasks(schemaMetadata.map { it.providerName })

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
    config = files("$projectDir/.detekt-config.yml")
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

    doLast {
        updateProviderSchemas(projectDir, versionConfigFile, skipPreReleaseVersions, fastForwardToMostRecentVersion)
    }
}

tasks.register<Task>("prepareReleaseAfterGeneratorUpdate") {
    group = "releaseManagement"
    doLast {
        updateGeneratorVersion(projectDir, versionConfigFile)
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
        generateLatestVersionsMarkdownTable(versionConfigFile)
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
    .map { "publishPulumi${it.providerName.capitalized()}PublicationToMavenCentralRepository" }

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

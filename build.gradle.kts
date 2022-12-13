import org.gradle.configurationcache.extensions.capitalized

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.20"
    id("org.jmailen.kotlinter") version "3.12.0"
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    id("code-generation")
}

group = "com.virtuslab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.pulumi:pulumi:0.6.0")

    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    implementation("com.squareup.tools.build:maven-archeologist:0.0.10")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("ch.qos.logback:logback-classic:1.4.5")

    implementation("com.google.code.gson:gson:2.10")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
    testImplementation(kotlin("test"))
    testImplementation("com.google.cloud:google-cloud-compute:1.15.0")
    testImplementation("io.kubernetes:client-java:16.0.2")
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("io.github.cdklabs:projen:0.65.42")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.virtuslab.pulumikotlin.codegen.MainKt")
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
    mainClass.set("com.virtuslab.pulumikotlin.scripts.ComputeSchemaSubsetScriptKt")
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
    doLast {
        updateProviderSchemas(projectDir, versionConfigFile)
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/VirtuslabRnD/pulumi-kotlin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

val publicationsToPublishToGitHub = schemaMetadata
    .filterNot { KotlinVersion.fromVersionString(it.kotlinVersion).isSnapshot }
    .map { "publishPulumi${it.providerName.capitalized()}PublicationToGitHubPackagesRepository" }

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        repository.name == "GitHubPackages" && publicationsToPublishToGitHub.contains(it.name)
    }
}

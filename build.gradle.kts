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

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
    testImplementation(kotlin("test"))
    testImplementation("com.google.cloud:google-cloud-compute:1.14.0")
    testImplementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("io.mockk:mockk:1.13.2")
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

val versionConfigFile = File("src/main/resources/version-config.json")
var schemaMetadata: List<SchemaMetadata> = getSchemaMetadata(versionConfigFile)

val createTasksForProvider: (String, String, String, String, List<String>) -> Unit by extra

schemaMetadata.forEach { schema ->
    createTasksForProvider(
        "build/generated-src",
        schema.providerName,
        schema.url,
        schema.kotlinVersion,
        schema.customDependencies,
    )
}

val createGlobalProviderTasks: (List<String>) -> Unit by extra

createGlobalProviderTasks(schemaMetadata.map { it.providerName })

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

tasks.register<Task>("postRelease") {
    group = "releaseManagement"
    doLast {
        tagRecentReleases(projectDir, versionConfigFile)
        replaceReleasedVersionsWithSnapshots(projectDir, versionConfigFile)
    }
}

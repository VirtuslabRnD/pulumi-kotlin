import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.0"
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
    implementation("com.pulumi:pulumi:(,1.0]")

    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.2")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")

    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    implementation("com.squareup.tools.build:maven-archeologist:0.0.10")

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.virtuslab.pulumikotlin.codegen.MainKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlinter {
    reporters = arrayOf("html", "plain")
}

detekt {
    buildUponDefaultConfig = true
    config = files("$projectDir/.detekt-config.yml")
}

tasks.withType<Jar> {
    archiveBaseName.set("${project.rootProject.name}-generator")
}

data class Schema(val providerName: String, val path: String, val customDependencies: List<String>)

val schemas = listOf(
    Schema("aws", "src/main/resources/aws-build-schema.json", listOf("com.pulumi:aws:5.14.0")),
    Schema("gcp", "src/main/resources/gcp-build-schema.json", listOf("com.pulumi:gcp:6.37.0")),
)

val createTasksForProvider: (String, String, String, List<String>) -> Unit by extra

schemas.forEach { schema ->
    createTasksForProvider("build/generated-src", schema.providerName, schema.path, schema.customDependencies)
}

val createGlobalProviderTasks: (List<String>) -> Unit by extra

createGlobalProviderTasks(schemas.map { it.providerName })

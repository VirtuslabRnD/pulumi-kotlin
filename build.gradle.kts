import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.7.0"
    id("org.jmailen.kotlinter") version "3.12.0"
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    id("code-generation")
}

group = "com.pulumi"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.pulumi:pulumi:(,1.0]")

    implementation("com.pulumi:aws:5.4.0")

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

tasks.withType<Jar> {
    archiveBaseName.set("${project.rootProject.name}-generator")
}

kotlinter {
    reporters = arrayOf("html", "plain")
}

detekt {
    buildUponDefaultConfig = true
    config = files("$projectDir/.detekt-config.yml")
}

val schemaMap = mapOf(
    "aws" to "src/main/resources/aws-schema.json",
    "gcp" to "src/main/resources/gcp-schema.json",
)

val createTasksForProvider: (String, String, String) -> Unit by extra

schemaMap.forEach { (provider, schemaPath) ->
    createTasksForProvider("build/generated-src", provider, schemaPath)
}

dependencies {
    "generatedAwsImplementation"("com.pulumi:aws:5.11.0-alpha.1658776797+e45bda97")
    "generatedGcpImplementation"("com.pulumi:gcp:6.38.0-alpha.1663342915+e285d89c")
}

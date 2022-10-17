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
    implementation("com.pulumi:pulumi:0.6.0")

    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    implementation("com.squareup.tools.build:maven-archeologist:0.0.10")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
    testImplementation(kotlin("test"))
    testImplementation("com.google.cloud:google-cloud-compute:1.12.1")
    testImplementation("org.apache.commons:commons-lang3:3.12.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.virtuslab.pulumikotlin.codegen.MainKt")
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
    Schema("aws", "src/main/resources/schema-aws-classic-subset-for-build.json", listOf("com.pulumi:aws:5.16.2")),
    Schema("gcp", "src/main/resources/schema-gcp-classic-subset-for-build.json", listOf("com.pulumi:gcp:6.38.0")),
)

val createTasksForProvider: (String, String, String, List<String>) -> Unit by extra

schemas.forEach { schema ->
    createTasksForProvider("build/generated-src", schema.providerName, schema.path, schema.customDependencies)
}

val createGlobalProviderTasks: (List<String>) -> Unit by extra

createGlobalProviderTasks(schemas.map { it.providerName })

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

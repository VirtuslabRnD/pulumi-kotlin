plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.0.21"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val ktorVersion by extra { "3.0.0" }

dependencies {
    implementation(kotlin("gradle-plugin", version = "2.0.21"))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.6.0")
    implementation("org.jmailen.gradle:kotlinter-gradle:4.4.1")

    implementation(kotlin("stdlib", "2.0.21"))
    implementation(kotlin("maven-serialization", "2.0.21"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0")
    
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    implementation("org.apache.maven:maven-artifact:3.9.9")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")
    implementation("org.semver4j:semver4j:5.4.1")

    testImplementation(kotlin("test", "2.0.21"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
}

tasks.test {
    useJUnitPlatform()
}

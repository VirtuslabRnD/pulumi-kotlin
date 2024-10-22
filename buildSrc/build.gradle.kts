plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.9.25"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val ktorVersion by extra { "2.3.11" }

dependencies {
    implementation(kotlin("gradle-plugin", version = "1.9.25"))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.6.0")
    implementation("org.jmailen.gradle:kotlinter-gradle:4.3.0")

    implementation(kotlin("stdlib", "1.9.25"))
    implementation(kotlin("maven-serialization", "1.9.25"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0")
    
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    implementation("org.apache.maven:maven-artifact:3.9.7")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
    implementation("org.semver4j:semver4j:5.3.0")

    testImplementation(kotlin("test", "1.9.25"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

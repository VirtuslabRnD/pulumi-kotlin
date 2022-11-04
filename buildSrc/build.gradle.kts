plugins {
    `kotlin-dsl`
    // This currently cannot be upgraded to 1.7.X
    // https://slack-chats.kotlinlang.org/t/544960/i-m-trying-to-upgrade-kotlin-to-1-7-0-and-i-m-no-longer-able
    kotlin("plugin.serialization") version "1.6.21"
}

repositories {
    mavenCentral()
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

val ktorVersion by extra { "2.1.3" }

dependencies {
    implementation(kotlin("gradle-plugin", version = "1.7.20"))
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation(kotlin("stdlib"))

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation(kotlin("maven-serialization"))
    implementation("org.apache.maven:maven-artifact:3.8.6")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation("org.semver4j:semver4j:2.2.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}

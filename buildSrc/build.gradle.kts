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

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")

    implementation("io.ktor:ktor-client-core:2.1.2")
    implementation("io.ktor:ktor-client-cio:2.1.2")
    implementation("io.ktor:ktor-client-logging:2.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.2")
    implementation("org.jetbrains.kotlin:kotlin-maven-serialization:1.7.20")
    implementation("org.apache.maven:maven-artifact:3.8.6")
    implementation("log4j:log4j:1.2.14")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("io.ktor:ktor-client-mock:2.1.2")
}

tasks.test {
    useJUnitPlatform()
}

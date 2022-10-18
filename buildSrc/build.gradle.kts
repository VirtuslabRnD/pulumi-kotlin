plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.10")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.2.1")
}

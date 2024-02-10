plugins {
    application
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.virtuslab:pulumi-kotlin:0.9.4.0")
}

application {
    mainClass.set(
        project.findProperty("mainClass") as? String ?: "myproject.App"
    )
}

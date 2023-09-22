plugins {
    application
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.virtuslab:pulumi-kotlin:0.9.4.0")
    implementation("org.virtuslab:pulumi-google-native-kotlin:0.31.1.1")
}

application {
    mainClass.set(
        project.findProperty("mainClass") as? String ?: "myproject.AppKt"
    )
}

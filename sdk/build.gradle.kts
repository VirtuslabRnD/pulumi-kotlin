import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka")
    signing
    id("io.gitlab.arturbosch.detekt")
    id("org.jmailen.kotlinter")
}

group = "org.virtuslab"
version = "0.11.0.1-SNAPSHOT"
base.archivesName.set("pulumi-kotlin-sdk")

repositories {
    mavenCentral()
}

dependencies {
    api("com.pulumi:pulumi:0.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation(kotlin("test", "1.9.22"))
}

tasks.test {
    useJUnitPlatform()
}

kotlinter {
    reporters = arrayOf("html", "plain")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("${rootProject.projectDir}/.detekt-config.yml")
}

task<Jar>("sourcesJar") {
    group = "build"
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

tasks.withType<DokkaTask> {
    moduleName.set("pulumi-kotlin")
}

task<Jar>("dokkaJavadocJar") {
    dependsOn(tasks["dokkaHtml"])
    group = "documentation"
    from(tasks["dokkaHtml"])
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("sonatype.username") as String?
                password = findProperty("sonatype.password") as String?
            }
        }
    }
    publications {
        create<MavenPublication>("pulumiKotlinSdk") {
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("dokkaJavadocJar"))
            from(components["java"])
            artifactId = "pulumi-kotlin"
        }

        publications
            .forEach {
                if (it is MavenPublication) {
                    configurePom(it)
                    if ((findProperty("signing.enabled") as String).toBoolean()) {
                        val signingKey = findProperty("signing.key") as String?
                        val signingKeyPassword = findProperty("signing.key.password") as String?

                        signing {
                            sign(it)
                            useInMemoryPgpKeys(signingKey, signingKeyPassword)
                        }
                    }
                }
            }
    }
}

fun configurePom(mavenPublication: MavenPublication) {
    mavenPublication.pom {
        name.set("Pulumi Kotlin")
        description.set(
            "Build cloud applications and infrastructure by combining the safety and reliability of infrastructure " +
                "as code with the power of the Kotlin programming language.",
        )
        url.set("https://github.com/VirtuslabRnD/pulumi-kotlin")
        inceptionYear.set("2022")

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/VirtuslabRnD/pulumi-kotlin/issues")
        }

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                name.set("Dariusz Dzikon")
                email.set("ddzikon@virtuslab.com")
                organization.set("VirtusLab")
            }
            developer {
                name.set("Michal Fudala")
                email.set("mfudala@virtuslab.com")
                organization.set("VirtusLab")
            }
            developer {
                name.set("Julia Plewa")
                email.set("jplewa@virtuslab.com")
                organization.set("VirtusLab")
            }
        }

        scm {
            url.set("https://github.com/VirtuslabRnD/pulumi-kotlin/tree/v$version")
            connection.set("scm:git:git://github.com/VirtuslabRnD/pulumi-kotlin.git")
            developerConnection.set("scm:git:ssh://github.com:VirtuslabRnD/pulumi-kotlin.git")
        }
    }
}

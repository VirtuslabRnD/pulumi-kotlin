import de.undercouch.gradle.tasks.download.Download
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.dokka.gradle.DokkaTask
import java.nio.file.Paths

plugins {
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka")
    id("de.undercouch.download")
}

val tasksToDisable: List<(String) -> String> = listOf(
    { sourceSetName: String -> "lintKotlin${sourceSetName.capitalized()}" },
)

val commonDependencies = listOf(
    "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.4.1",
    "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4",
)

val rootDir = project.rootDir.absolutePath
val outputDirectory = Paths.get(rootDir, "build", "generated-src").toFile()

val createTasksForProvider by extra {
    fun(schema: SchemaMetadata) {
        val providerName = schema.providerName
        val schemaUrl = schema.url
        val version = schema.kotlinVersion
        val javaLibraryDependency = "com.pulumi:$providerName:${schema.javaVersion}"

        val sourceSetName = "pulumi${providerName.capitalized()}"
        val generationTaskName = "generate${sourceSetName.capitalized()}Sources"
        val compilationTaskName = "compile${sourceSetName.capitalized()}Kotlin"
        val jarTaskName = "${sourceSetName}Jar"
        val implementationDependency = "${sourceSetName}Implementation"
        val archiveName = "pulumi-$providerName-kotlin"
        val sourcesJarTaskName = "${sourceSetName}SourcesJar"
        val formatTaskName = "formatKotlin${sourceSetName.capitalized()}"
        val javadocGenerationTaskName = "dokka${sourceSetName.capitalized()}Javadoc"
        val javadocJarTaskName = "dokka${sourceSetName.capitalized()}JavadocJar"
        val sourcesPublicationName = "${sourceSetName}Sources"
        val javadocPublicationName = "${sourceSetName}Javadoc"
        val downloadTaskName = "download${providerName.capitalized()}Schema"

        val schemaDownloadPath = Paths.get(rootDir, "build", "tmp", "schema", "$providerName-$version.json").toFile()

        createDownloadTask(downloadTaskName, schemaUrl, schemaDownloadPath)
        createGenerationTask(generationTaskName, downloadTaskName, schemaDownloadPath, outputDirectory, providerName)
        createSourceSet(sourceSetName, outputDirectory, providerName)

        tasks[generationTaskName].finalizedBy(tasks[formatTaskName])
        tasks[generationTaskName].finalizedBy(tasks[compilationTaskName])

        createJarTask(jarTaskName, generationTaskName, sourceSetName, archiveName, version)
        createSourcesJarTask(sourcesJarTaskName, generationTaskName, sourceSetName, archiveName, version)
        createJavadocGenerationTask(javadocGenerationTaskName, generationTaskName, archiveName, sourceSetName, version)
        createJavadocJarTask(javadocJarTaskName, javadocGenerationTaskName, archiveName, version)

        configurePublication(
            sourceSetName,
            listOf(jarTaskName, sourcesJarTaskName, javadocJarTaskName,),
            archiveName,
            version,
            listOf(sourceSetName, sourcesPublicationName, javadocPublicationName),
            implementationDependency,
        )

        tasksToDisable.forEach {
            tasks[it(sourceSetName)].enabled = false
        }

        val customDependencies = schema.customDependencies + listOf(javaLibraryDependency) + commonDependencies
        customDependencies.forEach {
            dependencies {
                implementationDependency(it)
            }
        }
    }
}

val createE2eTasksForProvider by extra {
    fun(schema: SchemaMetadata) {
        val providerName = schema.providerName
        val schemaUrl = schema.url
        val version = schema.kotlinVersion
        val javaLibraryDependency = "com.pulumi:$providerName:${schema.javaVersion}"

        val sourceSetName = "pulumi${providerName.capitalized()}E2e"
        val generationTaskName = "generate${sourceSetName.capitalized()}Sources"
        val compilationTaskName = "compile${sourceSetName.capitalized()}Kotlin"
        val jarTaskName = "${sourceSetName}Jar"
        val implementationDependency = "${sourceSetName}Implementation"
        val archiveName = "pulumi-$providerName-kotlin"
        val downloadTaskName = "download${providerName.capitalized()}E2eSchema"

        val schemaDownloadPath =
            Paths.get(rootDir, "build", "tmp", "schema", "e2e", "$providerName-$version.json").toFile()

        createDownloadTask(downloadTaskName, schemaUrl, schemaDownloadPath)
        createGenerationTask(generationTaskName, downloadTaskName, schemaDownloadPath, outputDirectory, providerName)
        createSourceSet(sourceSetName, outputDirectory, providerName)

        tasks[generationTaskName].finalizedBy(tasks[compilationTaskName])

        createJarTask(jarTaskName, generationTaskName, sourceSetName, archiveName, version)

        configurePublication(
            sourceSetName,
            listOf(jarTaskName),
            archiveName,
            version,
            listOf(sourceSetName),
            implementationDependency,
        )

        tasksToDisable.forEach {
            tasks[it(sourceSetName)].enabled = false
        }

        val customDependencies = schema.customDependencies + listOf(javaLibraryDependency) + commonDependencies
        customDependencies.forEach {
            dependencies {
                implementationDependency(it)
            }
        }
    }
}

val createGlobalProviderTasks by extra {
    fun(providerNames: List<String>) {
        task("generatePulumiSources") {
            dependsOn(providerNames.map { tasks["generatePulumi${it.capitalized()}Sources"] })
            group = "generation"
        }
    }
}

fun createDownloadTask(
    taskName: String,
    schemaUrl: String,
    schemaDownloadPath: File,
) {
    task<Download>(taskName) {
        src(schemaUrl)
        dest(schemaDownloadPath.canonicalPath)
    }
}

fun createGenerationTask(
    generationTaskName: String,
    downloadTaskName: String,
    schemaDownloadPath: File,
    outputDirectory: File,
    providerName: String,
) {
    task<JavaExec>(generationTaskName) {
        dependsOn(tasks[downloadTaskName])
        classpath = project.sourceSets["main"].runtimeClasspath
        group = "generation"
        mainClass.set("com.virtuslab.pulumikotlin.codegen.MainKt")
        setArgsString(
            "--schema-path $schemaDownloadPath " +
                "--output-directory-path ${File(outputDirectory, providerName)}",
        )
    }
}

fun createSourceSet(sourceSetName: String, outputDirectory: File, providerName: String) {
    project.sourceSets {
        create(sourceSetName) {
            java {
                srcDir(File(outputDirectory, providerName))
                compileClasspath += sourceSets["main"].compileClasspath
            }
        }
    }
}

fun createJarTask(
    jarTaskName: String,
    generationTaskName: String,
    sourceSetName: String,
    archiveName: String,
    version: String,
) {
    task<Jar>(jarTaskName) {
        dependsOn(tasks[generationTaskName])
        group = "build"
        from(project.the<SourceSetContainer>()[sourceSetName].output)
        archiveBaseName.set(archiveName)
        archiveVersion.set(version)
        // This setting is needed to enable building JAR archives with more than 65535 files, e.g. the compiled
        // Google Native schema. See:
        // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html#org.gradle.api.tasks.bundling.Jar:zip64
        isZip64 = true
    }
}

fun createSourcesJarTask(
    sourcesJarTaskName: String,
    generationTaskName: String,
    sourceSetName: String,
    archiveName: String,
    version: String,
) {
    task<Jar>(sourcesJarTaskName) {
        dependsOn(tasks[generationTaskName])
        group = "build"
        from(project.the<SourceSetContainer>()[sourceSetName].allSource)
        archiveBaseName.set(archiveName)
        archiveVersion.set(version)
        archiveClassifier.set("sources")
        // This setting is needed to enable building JAR archives with more than 65535 files. See:
        // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html#org.gradle.api.tasks.bundling.Jar:zip64
        isZip64 = true
    }
}

fun createJavadocGenerationTask(
    javadocGenerationTaskName: String,
    generationTaskName: String,
    archiveName: String,
    sourceSetName: String,
    version: String,
) {
    task<DokkaTask>(javadocGenerationTaskName) {
        dependsOn(tasks[generationTaskName])
        moduleName.set(archiveName)
        moduleVersion.set(version)
        dokkaSourceSets {
            named("main") {
                suppress.set(true)
            }
            named(sourceSetName) {
                suppress.set(false)
            }
        }
    }
}

fun createJavadocJarTask(
    javadocJarTaskName: String,
    javadocGenerationTaskName: String,
    archiveName: String,
    version: String,
) {
    task<Jar>(javadocJarTaskName) {
        dependsOn(tasks[javadocGenerationTaskName])
        group = "documentation"
        from(tasks[javadocGenerationTaskName])
        archiveBaseName.set(archiveName)
        archiveVersion.set(version)
        archiveClassifier.set("javadoc")
        // This setting is needed to enable building JAR archives with more than 65535 files, e.g. Dokka docs for
        // the full GCP schema. See:
        // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html#org.gradle.api.tasks.bundling.Jar:zip64
        isZip64 = true
    }
}

fun configurePublication(
    sourceSetName: String,
    artifacts: List<String>,
    archiveName: String,
    version: String,
    publicationNames: List<String>,
    implementationDependency: String,
) {
    publishing {
        publications {
            create<MavenPublication>(sourceSetName) {
                artifacts.forEach {
                    artifact(tasks[it])
                }
                artifactId = archiveName
                setVersion(version)
            }

            publications
                .filter { it.name in publicationNames }
                .forEach {
                    if (it is MavenPublication) {
                        configurePom(it, implementationDependency)
                    }
                }
        }
    }
}

fun configurePom(
    mavenPublication: MavenPublication,
    implementationDependency: String,
) {
    mavenPublication.pom {
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        withXml {
            val dependenciesNode = asNode().appendNode("dependencies")
            configurations[implementationDependency].dependencies
                .forEach {
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
                }
        }
    }
}

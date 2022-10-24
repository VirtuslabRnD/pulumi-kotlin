import de.undercouch.gradle.tasks.download.Download
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka")
    id("de.undercouch.download")
}

val tasksToDisable: List<(String) -> String> = listOf(
    { sourceSetName: String -> "lintKotlin${sourceSetName.capitalized()}" },
)

val createTasksForProvider by extra {
    fun(outputDirectory: String, schema: SchemaMetadata) {
        val providerName = schema.providerName
        val schemaUrl = schema.url
        val version = schema.kotlinVersion
        val customDependencies = schema.customDependencies

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
        val schemaDownloadPath = "build/tmp/schema/$providerName-$version.json"

        createDownloadTask(downloadTaskName, schemaUrl, schemaDownloadPath)
        createGenerationTask(generationTaskName, downloadTaskName, schemaDownloadPath, outputDirectory, providerName)
        createSourceSet(sourceSetName, outputDirectory, providerName)

        tasks[generationTaskName].finalizedBy(tasks[formatTaskName])
        tasks[generationTaskName].finalizedBy(tasks[compilationTaskName])

        createJarTask(jarTaskName, generationTaskName, sourceSetName, archiveName)
        createSourcesJarTask(sourcesJarTaskName, generationTaskName, sourceSetName, archiveName)

        // TODO: Remove this once it becomes possible to generate Dokka docs for GCP
        //  on GitHub Actions without getting an OOM error.
        if (providerName != "gcp") {
            createJavadocGenerationTask(javadocGenerationTaskName, generationTaskName, archiveName, sourceSetName)
            createJavadocJarTask(javadocJarTaskName, javadocGenerationTaskName, archiveName)
        }

        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/VirtuslabRnD/pulumi-kotlin")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }

            publications {
                createPublication(this, sourceSetName, jarTaskName, archiveName, version)
                createSourcesPublication(this, sourcesPublicationName, sourcesJarTaskName, archiveName, version)
                // TODO: Remove this once it becomes possible to generate Dokka docs for GCP
                //  on GitHub Actions without getting an OOM error.
                if (providerName != "gcp") {
                    createJavadocPublication(this, javadocPublicationName, javadocJarTaskName, archiveName, version)
                }

                publications
                    .filter { it.name in listOf(sourceSetName, sourcesPublicationName, javadocPublicationName) }
                    .forEach {
                        if (it is MavenPublication) {
                            configurePom(it, implementationDependency)
                        }
                    }
            }
        }

        tasksToDisable.forEach {
            tasks[it(sourceSetName)].enabled = false
        }

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

fun Code_generation_gradle.createDownloadTask(
    taskName: String,
    schemaUrl: String,
    schemaDownloadPath: String,
) {
    task<Download>(taskName) {
        src(schemaUrl)
        dest(schemaDownloadPath)
    }
}

fun Code_generation_gradle.createGenerationTask(
    generationTaskName: String,
    downloadTaskName: String,
    schemaDownloadPath: String,
    outputDirectory: String,
    providerName: String,
) {
    task<JavaExec>(generationTaskName) {
        dependsOn(tasks[downloadTaskName])
        classpath = project.sourceSets["main"].runtimeClasspath
        group = "generation"
        mainClass.set("com.virtuslab.pulumikotlin.codegen.MainKt")
        setArgsString("--schema-path $schemaDownloadPath --output-directory-path $outputDirectory/$providerName")
    }
}

fun Code_generation_gradle.createSourceSet(sourceSetName: String, outputDirectory: String, providerName: String) {
    project.sourceSets {
        create(sourceSetName) {
            java {
                srcDir("$outputDirectory/$providerName")
                compileClasspath += sourceSets["main"].compileClasspath
            }
        }
    }
}

fun Code_generation_gradle.createJarTask(
    jarTaskName: String,
    generationTaskName: String,
    sourceSetName: String,
    archiveName: String,
) {
    task<Jar>(jarTaskName) {
        dependsOn(tasks[generationTaskName])
        group = "build"
        from(project.the<SourceSetContainer>()[sourceSetName].output)
        archiveBaseName.set(archiveName)
    }
}

fun Code_generation_gradle.createSourcesJarTask(
    sourcesJarTaskName: String,
    generationTaskName: String,
    sourceSetName: String,
    archiveName: String,
) {
    task<Jar>(sourcesJarTaskName) {
        dependsOn(tasks[generationTaskName])
        group = "build"
        from(project.the<SourceSetContainer>()[sourceSetName].allSource)
        archiveBaseName.set(archiveName)
        archiveClassifier.set("sources")
    }
}

fun Code_generation_gradle.createJavadocGenerationTask(
    javadocGenerationTaskName: String,
    generationTaskName: String,
    archiveName: String,
    sourceSetName: String,
) {
    task<DokkaTask>(javadocGenerationTaskName) {
        dependsOn(tasks[generationTaskName])
        moduleName.set(archiveName)
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

fun Code_generation_gradle.createJavadocJarTask(
    javadocJarTaskName: String,
    javadocGenerationTaskName: String,
    archiveName: String,
) {
    task<Jar>(javadocJarTaskName) {
        dependsOn(tasks[javadocGenerationTaskName])
        group = "documentation"
        from(tasks[javadocGenerationTaskName])
        archiveBaseName.set(archiveName)
        archiveClassifier.set("javadoc")
        // This setting is needed to enable building JAR archives with more than 65535 files, e.g. Dokka docs for
        // the full GCP schema. See:
        // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html#org.gradle.api.tasks.bundling.Jar:zip64
        isZip64 = true
    }
}

fun createPublication(
    publicationContainer: PublicationContainer,
    sourceSetName: String,
    jarTaskName: String,
    archiveName: String,
    version: String,
) {
    publicationContainer.create<MavenPublication>(sourceSetName) {
        artifact(tasks[jarTaskName])
        artifactId = archiveName
        setVersion(version)
    }
}

fun createSourcesPublication(
    publicationContainer: PublicationContainer,
    sourcesPublicationName: String,
    sourcesJarTaskName: String,
    archiveName: String,
    version: String,
) {
    publicationContainer.create<MavenPublication>(sourcesPublicationName) {
        artifact(tasks[sourcesJarTaskName])
        artifactId = archiveName
        setVersion(version)
    }
}

fun createJavadocPublication(
    publicationContainer: PublicationContainer,
    javadocPublicationName: String,
    javadocJarTaskName: String,
    archiveName: String,
    version: String,
) {
    publicationContainer.create<MavenPublication>(javadocPublicationName) {
        artifact(tasks[javadocJarTaskName])
        artifactId = archiveName
        setVersion(version)
    }
}

fun Code_generation_gradle.configurePom(
    mavenPublication: MavenPublication,
    implementationDependency: String,
) {
    mavenPublication.pom {
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

import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka")
}

val tasksToDisable: List<(String) -> String> = listOf(
    { sourceSetName: String -> "lintKotlin${sourceSetName.capitalized()}" },
)

val createTasksForProvider by extra {
    fun(outputDirectory: String, providerName: String, schemaPath: String, customDependencies: List<String>) {
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

        tasks.register<JavaExec>(generationTaskName) {
            classpath = project.sourceSets["main"].runtimeClasspath
            group = "generation"
            mainClass.set("com.virtuslab.pulumikotlin.codegen.MainKt")
            setArgsString("--schema-path $schemaPath --output-directory-path $outputDirectory/$providerName")
        }

        project.sourceSets {
            create(sourceSetName) {
                java {
                    srcDir("$outputDirectory/$providerName")
                    compileClasspath += sourceSets["main"].compileClasspath
                }
            }
        }

        tasks[generationTaskName].finalizedBy(tasks[formatTaskName])
        tasks[generationTaskName].finalizedBy(tasks[compilationTaskName])

        tasks.register<Jar>(jarTaskName) {
            dependsOn(tasks[generationTaskName])
            group = "build"
            from(project.the<SourceSetContainer>()[sourceSetName].output)
            archiveBaseName.set(archiveName)
        }

        tasks.register<Jar>(sourcesJarTaskName) {
            dependsOn(tasks[generationTaskName])
            group = "build"
            from(project.the<SourceSetContainer>()[sourceSetName].allSource)
            archiveBaseName.set(archiveName)
            archiveClassifier.set("sources")
        }

        tasks.register<DokkaTask>(javadocGenerationTaskName) {
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

        tasks.register<Jar>(javadocJarTaskName) {
            dependsOn(tasks[javadocGenerationTaskName])
            group = "documentation"
            from(tasks[javadocGenerationTaskName])
            archiveBaseName.set(archiveName)
            archiveClassifier.set("javadoc")
        }

        publishing {
            publications {
                create<MavenPublication>(sourceSetName) {
                    artifact(tasks[jarTaskName])
                    artifactId = archiveName
                    pom {
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
                create<MavenPublication>("${sourceSetName}Sources") {
                    artifact(tasks[sourcesJarTaskName])
                    artifactId = archiveName
                }
                create<MavenPublication>("${sourceSetName}Javadoc") {
                    artifact(tasks[javadocJarTaskName])
                    artifactId = archiveName
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
        tasks.register("generatePulumiSources") {
            dependsOn(providerNames.map { tasks["generatePulumi${it.capitalized()}Sources"] })
            group = "generation"
        }
    }
}

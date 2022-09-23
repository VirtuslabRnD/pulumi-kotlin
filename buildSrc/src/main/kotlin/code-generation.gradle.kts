import org.gradle.configurationcache.extensions.capitalized

plugins {
    `java-library`
    `maven-publish`
}

val tasksToDisable: List<(String) -> String> = listOf(
    { sourceSetName: String -> "lintKotlin${sourceSetName.capitalized()}" },
    { sourceSetName: String -> "formatKotlin${sourceSetName.capitalized()}" },
)

val createTasksForProvider by extra {
    fun(outputDirectory: String, providerName: String, schemaPath: String, customDependencies: List<String>) {
        val sourceSetName = "generated${providerName.capitalized()}"
        val generationTaskName = "generate${providerName.capitalized()}Sources"
        val compilationTaskName = "compile${sourceSetName.capitalized()}Kotlin"
        val jarTaskName = "${sourceSetName}SourcesJar"
        val implementationDependency = "${sourceSetName}Implementation"

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

        tasks[generationTaskName].finalizedBy(tasks[compilationTaskName])

        tasks.register<Jar>(jarTaskName) {
            dependsOn(tasks[generationTaskName])
            group = "build"
            from(project.the<SourceSetContainer>()[sourceSetName].output)
            archiveBaseName.set("${project.rootProject.name}-$providerName")
        }

        publishing {
            publications {
                create<MavenPublication>(sourceSetName) {
                    artifact(tasks[jarTaskName])
                    artifactId = "${project.rootProject.name}-$providerName"
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
        tasks.register("generateSources") {
            dependsOn(providerNames.map { tasks["generate${it.capitalized()}Sources"] })
            group = "generation"
        }

        tasks.register("generatedSourcesJar") {
            dependsOn(providerNames.map { tasks["generated${it.capitalized()}SourcesJar"] })
            group = "build"
        }

        tasks.register("compileGeneratedJava") {
            dependsOn(providerNames.map { tasks["compileGenerated${it.capitalized()}Java"] })
        }

        tasks.register("compileGeneratedKotlin") {
            dependsOn(providerNames.map { tasks["compileGenerated${it.capitalized()}Kotlin"] })
        }
    }
}

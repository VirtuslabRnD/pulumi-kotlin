import org.gradle.configurationcache.extensions.capitalized

plugins {
    `java-library`
    `maven-publish`
}

val createTasksForProvider by extra {
    fun(outputDirectory: String, provider: String, schemaPath: String) {
        val sourceSetName = "generated${provider.capitalized()}"
        val generationTaskName = "generate${provider.capitalized()}Sources"
        val compilationTaskName = "compile${sourceSetName.capitalized()}Kotlin"
        val jarTaskName = "${sourceSetName}SourcesJar"

        tasks.register<JavaExec>(generationTaskName) {
            classpath = project.sourceSets["main"].runtimeClasspath
            group = "build"
            mainClass.set("com.virtuslab.pulumikotlin.codegen.MainKt")
            setArgsString("--schema-path $schemaPath --output-directory-path $outputDirectory/$provider")
        }

        project.sourceSets {
            create(sourceSetName) {
                java {
                    srcDir("$outputDirectory/$provider")
                    compileClasspath += sourceSets["main"].compileClasspath
                }
            }
        }

        tasks[generationTaskName].finalizedBy(tasks[compilationTaskName])

        tasks.register<Jar>(jarTaskName) {
            dependsOn(tasks[generationTaskName])
            group = "build"
            from(project.the<SourceSetContainer>()[sourceSetName].output)
            archiveBaseName.set("${project.rootProject.name}-$provider")
        }

        publishing {
            publications {
                create<MavenPublication>(sourceSetName) {
                    artifact(tasks[jarTaskName])
                    artifactId = "${project.rootProject.name}-$provider"
                }
            }
        }

        tasks["lintKotlin${sourceSetName.capitalized()}"].enabled = false
        tasks["formatKotlin${sourceSetName.capitalized()}"].enabled = false
    }
}

package com.virtuslab.pulumikotlin

import com.pulumi.core.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiNamingConfiguration
import io.github.cdklabs.projen.java.JavaProject
import io.github.cdklabs.projen.java.JavaProjectOptions
import org.junit.jupiter.api.Assertions.assertFalse
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal fun <T> extractOutputValue(output: Output<T>?): T? {
    var value: T? = null
    output?.applyValue { value = it }
    return value
}

internal fun <T> concat(iterableOfIterables: Iterable<Iterable<T>>?): List<T> =
    iterableOfIterables?.flatten().orEmpty()

internal fun <T> concat(vararg iterables: Iterable<T>?): List<T> =
    concat(iterables.filterNotNull().asIterable())

internal fun namingConfigurationWithSlashInModuleFormat(providerName: String) =
    PulumiNamingConfiguration.create(providerName = providerName, moduleFormat = "(.*)(?:/[^/]*)")

private fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

internal fun <T> assertDoesNotContain(iterable: Iterable<T>, element: T, message: String? = null) {
    val prefix = messagePrefix(message)
    assertFalse(iterable.contains(element)) {
        """
            $prefix Expected the collection to not contain the element.
            Collection <$iterable>, element <$element>.
        """.trimIndent()
    }
}

private fun getFileDirectoryForFilePackage(file: File): String {
    val packageString = file.readLines()
        .firstOrNull { it.startsWith("package") }
        ?.removePrefix("package")
        ?.removeSuffix(";")
        ?.trim()

    return packageString?.replace(".", "/") ?: "com/main"
}

private fun organizeFilesAccordingToTheirPackage(filesToOrganize: List<File>, outputDirectory: File) {
    filesToOrganize.forEach { file ->
        val relativeDirectory = getFileDirectoryForFilePackage(file)
        val absoluteDirectory = File(outputDirectory, relativeDirectory)
        val absoluteTargetFile = File(absoluteDirectory, file.name)
        absoluteDirectory.mkdirs()
        file.copyTo(absoluteTargetFile)
    }
}

internal fun ejectToMavenProject(dependencies: List<String>, directoriesOrFiles: List<File>, testName: String? = null) {
    val formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"))
    val preparedTestName = testName?.let {
        val withEscapedChars = it.replace(Regex(" +"), "-").replace(Regex("[^a-zA-Z0-9-]"), "")
        "$withEscapedChars-"
    }
        .orEmpty()

    val ejectedProjectName = "test-$preparedTestName$formattedTime"
    val outputDirectoryBase = "ejected-tests/$ejectedProjectName"

    val project = JavaProject(
        JavaProjectOptions.builder()
            .artifactId(ejectedProjectName)
            .groupId("com.virtuslab")
            .name(ejectedProjectName)
            .version("1.0.0")
            .projenrcJava(true)
            .deps(dependencies.map { it.replaceFirst(":", "/").replace(":", "@") })
            .outdir(outputDirectoryBase)
            .build(),
    )

    println("Ejecting project to $outputDirectoryBase...")
    project.synth()

    val filesToOrganize = directoriesOrFiles.flatMap { listFilesRecursively(it) }
    val outputDirectory = File(outputDirectoryBase).resolve("src/main/java")
    organizeFilesAccordingToTheirPackage(filesToOrganize, outputDirectory)
}

fun listFilesRecursively(directory: File): List<File> {
    require(directory.isDirectory) { "Expected directory (got: $directory)" }
    val files = directory.listFiles()?.toList().orEmpty()
    val filesFoundRecursively = files.filter { it.isDirectory }.flatMap { listFilesRecursively(it) }
    val filesFoundInCurrentDir = files.filter { it.isFile }
    return filesFoundInCurrentDir + filesFoundRecursively
}

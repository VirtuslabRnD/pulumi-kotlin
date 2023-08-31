package org.virtuslab.pulumikotlin

import com.pulumi.core.Output
import io.github.cdklabs.projen.java.JavaProject
import io.github.cdklabs.projen.java.JavaProjectOptions
import org.junit.jupiter.api.Assertions.assertFalse
import org.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiName
import org.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiNamingConfiguration
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal fun <T> extractOutputValue(output: Output<T>?): T? {
    var value: T? = null
    output?.applyValue { value = it }
    return value
}

internal fun namingConfigurationWithSlashInModuleFormat(
    providerName: String,
    packageOverrides: Map<String, String> = emptyMap(),
) =
    PulumiNamingConfiguration.create(
        providerName = providerName,
        moduleFormat = "(.*)(?:/[^/]*)",
        packageOverrides = packageOverrides,
    )

internal fun pulumiName(providerName: String, baseNamespace: List<String>, moduleName: String, name: String) =
    PulumiName(providerName, null, baseNamespace, moduleName, name, false)

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

internal fun ejectToMavenProject(directoriesOrFiles: List<File>, testName: String? = null) {
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
            .groupId("org.virtuslab")
            .name(ejectedProjectName)
            .version("1.0.0")
            .projenrcJava(true)
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

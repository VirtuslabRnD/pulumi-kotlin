package org.virtuslab.pulumikotlin.codegen.step3codegen

import org.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import org.virtuslab.pulumikotlin.codegen.step3codegen.functions.FunctionGenerator
import org.virtuslab.pulumikotlin.codegen.step3codegen.resources.ResourceGenerator
import org.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator
import org.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator.GenerationOptions
import org.virtuslab.pulumikotlin.codegen.utils.Paths
import java.io.File

data class GeneratorArguments(
    val types: List<RootType>,
    val sdkFilesToCopyPath: String = Paths.filesToCopyToSdkPath,
    val resources: List<ResourceType> = emptyList(),
    val functions: List<FunctionType> = emptyList(),
    val options: GenerationOptions = GenerationOptions(),
)

object CodeGenerator {
    fun run(input: GeneratorArguments): List<WriteableFile> {
        val typeNameClashResolver = TypeNameClashResolver(input.types)
        val generatedTypes = TypeGenerator.generateTypes(input.types, input.options, typeNameClashResolver)
        val generatedResources = ResourceGenerator.generateResources(input.resources, typeNameClashResolver)
        val generatedFunctions = FunctionGenerator.generateFunctions(input.functions, typeNameClashResolver)

        val allGeneratedFileSpecs = generatedTypes + generatedResources + generatedFunctions
        val allGeneratedFiles = allGeneratedFileSpecs.map { InMemoryGeneratedFile(it) }

        val existingFiles = listExistingFiles(input.sdkFilesToCopyPath)
            .map { ExistingFile(it.absolutePath, withPulumiPackagePrefix(it)) }

        return allGeneratedFiles + existingFiles
    }

    private fun withPulumiPackagePrefix(it: File) = "com/pulumi/kotlin/${it.name}"

    private fun listExistingFiles(path: String): List<File> {
        return File(path).listFiles().orEmpty()
            .flatMap { if (it.isDirectory) listExistingFiles(it.absolutePath) else listOf(it) }
    }
}

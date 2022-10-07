package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.RootType
import com.virtuslab.pulumikotlin.codegen.step3codegen.functions.FunctionGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.resources.ResourceGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator.GenerationOptions
import com.virtuslab.pulumikotlin.codegen.utils.Paths
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
        val generatedTypes = TypeGenerator.generateTypes(input.types, input.options)
        val generatedResources = ResourceGenerator.generateResources(input.resources)
        val generatedFunctions = FunctionGenerator.generateFunctions(input.functions)

        val allGeneratedFileSpecs = generatedTypes + generatedResources + generatedFunctions
        val allGeneratedFiles = allGeneratedFileSpecs.map { InMemoryGeneratedFile(it) }

        val existingFiles = File(input.sdkFilesToCopyPath)
            .listFiles()
            .orEmpty()
            .map { ExistingFile(it.absolutePath, withPulumiPackagePrefix(it)) }

        return allGeneratedFiles + existingFiles
    }

    private fun withPulumiPackagePrefix(it: File) = "com/pulumi/kotlin/${it.name}"
}

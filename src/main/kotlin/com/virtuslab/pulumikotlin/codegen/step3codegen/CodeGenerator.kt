package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.virtuslab.pulumikotlin.codegen.step2intermediate.AutonomousType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import com.virtuslab.pulumikotlin.codegen.step3codegen.functions.FunctionsGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.resources.ResourcesGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.TypesGenerator
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.TypesGenerator.FeatureFlags
import com.virtuslab.pulumikotlin.codegen.utils.Paths
import java.io.File

data class GeneratorArguments(
        val types: List<AutonomousType>,
        val sdkFilesToCopyPath: String = Paths.filesToCopyToSdkPath,
        val resources: List<ResourceType> = emptyList(),
        val functions: List<FunctionType> = emptyList(),
        val featureFlags: FeatureFlags = FeatureFlags(),
)

object CodeGenerator {
    fun run(input: GeneratorArguments): List<WriteableFile> {
        val generatedTypes = TypesGenerator.generateTypes(input.types, input.featureFlags)
        val generatedResources = ResourcesGenerator.generateResources(input.resources)
        val generatedFunctions = FunctionsGenerator.generateFunctions(input.functions)

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

package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AutonomousType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput
import com.virtuslab.pulumikotlin.codegen.step2intermediate.MoreTypes
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ResourceType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic
import com.virtuslab.pulumikotlin.codegen.utils.Paths
import java.io.File

data class GeneratorArguments(
    val types: List<AutonomousType>,
    val sdkFilesToCopyPath: String = Paths.filesToCopyToSdkPath,
    val resources: List<ResourceType> = emptyList(),
    val functions: List<FunctionType> = emptyList(),
    val options: GenerationOptions = GenerationOptions(),
)

object CodeGenerator {
    fun run(input: GeneratorArguments): List<WriteableFile> {
        val generatedTypes = input.types.filterIsInstance<ComplexType>().map { a ->
            when {
                a.metadata.useCharacteristic.toNested() == UseCharacteristic.FunctionNested || a.metadata.inputOrOutput == InputOrOutput.Output -> {
                    generateTypeWithNiceBuilders(
                        a.metadata,
                        a.fields.map { (name, type) ->
                            Field(
                                name,
                                NormalField(type.type) { expr -> expr },
                                type.required,
                                overloads = emptyList(),
                            )
                        },
                        input.options,
                    )
                }

                else -> {
                    generateTypeWithNiceBuilders(
                        a.metadata,
                        a.fields.map { (name, type) ->
                            Field(
                                name,
                                OutputWrappedField(type.type),
                                type.required,
                                listOf(
                                    NormalField(type.type) { argument -> MoreTypes.Java.Pulumi.Output.of(argument) },
                                ),
                            )
                        },
                        input.options,
                    )
                }
            }
        }

        val generatedResources = generateResources(input.resources)
        val generatedFunctions = generateFunctions(input.functions)

        val generatedFiles =
            (generatedTypes + generatedResources + generatedFunctions).map { InMemoryGeneratedFile(it) }

        val existingFiles = File(input.sdkFilesToCopyPath)
            .listFiles()
            .orEmpty()
            .map { ExistingFile(it.absolutePath, withPulumiPackagePrefix(it)) }

        return generatedFiles + existingFiles
    }

    private fun withPulumiPackagePrefix(it: File) = "com/pulumi/kotlin/${it.name}"
}

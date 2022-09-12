package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import com.virtuslab.pulumikotlin.codegen.expressions.invoke
import com.virtuslab.pulumikotlin.codegen.utils.Paths
import java.io.File

object CodeGenerator {
    fun run(input: Arguments): List<WriteableFile> {
        val generatedTypes = input.types.filterIsInstance<ComplexType>().map { a ->
            when {
                a.metadata.useCharacteristic.toNested() == UseCharacteristic.FunctionNested || a.metadata.inputOrOutput == InputOrOutput.Output -> {
                    TypeGenerator.generateTypes(
                        a.metadata,
                        a.fields.map { (name, type) ->
                            Field(
                                name,
                                NormalField(type.type) { expr -> expr },
                                type.required,
                                overloads = emptyList()
                            )
                        },
                        input.options
                    )
                }

                else -> {
                    TypeGenerator.generateTypes(
                        a.metadata,
                        a.fields.map { (name, type) ->
                            Field(name, OutputWrappedField(type.type), type.required,
                                listOf(
                                    NormalField(type.type) { argument -> MoreTypes.Java.Pulumi.Output.of(argument) }
                                )
                            )
                        },
                        input.options
                    )
                }
            }
        }

        val generatedResources = ResourceGenerator.generateResources(input.resources)
        val generatedFunctions = FunctionGenerator.generateFunctions(input.functions)

        val generatedFiles =
            (generatedTypes + generatedResources + generatedFunctions).map { InMemoryGeneratedFile(it) }

        val existingFiles = File(input.sdkFilesToCopyPath)
            .listFiles()
            .orEmpty()
            .map { ExistingFile(it.absolutePath, withPulumiPackagePrefix(it)) }

        return generatedFiles + existingFiles
    }

    private fun withPulumiPackagePrefix(it: File) = "com/pulumi/kotlin/${it.name}"

    data class Arguments(
        val types: List<AutonomousType>,
        val sdkFilesToCopyPath: String = Paths.filesToCopyToSdkPath,
        val resources: List<ResourceType> = emptyList(),
        val functions: List<FunctionType> = emptyList(),
        val options: TypeGenerator.Options = TypeGenerator.Options()
    )
}

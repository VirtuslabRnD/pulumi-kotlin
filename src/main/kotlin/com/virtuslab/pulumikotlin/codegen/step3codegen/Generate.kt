package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.CodeBlock
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import java.io.File

object Generate {
    fun generate(types: List<AutonomousType>, resources: List<ResourceType> = emptyList(), options: GenerationOptions = GenerationOptions()): List<WriteableFile> {
        val generatedTypes = types.filterIsInstance<ComplexType>().map { a ->
            when {
                a.metadata.useCharacteristic.toNested() == UseCharacteristic.FunctionNested || a.metadata.inputOrOutput == InputOrOutput.Output -> {
                    generateTypeWithNiceBuilders(
                        a.metadata,
                        a.fields.map { (name, type) ->
                            Field(
                                name,
                                NormalField(type.type) { from, to -> CodeBlock.of("val $to = $from") },
                                type.required,
                                overloads = emptyList()
                            )
                        },
                        options
                    )
                }

                else -> {
                    generateTypeWithNiceBuilders(
                        a.metadata,
                        a.fields.map { (name, type) ->
                            Field(name, OutputWrappedField(type.type), type.required,
                                listOf(
                                    NormalField(type.type) { from, to -> CodeBlock.of("val $to = Output.of($from)") }
                                )
                            )
                        },
                        options
                    )
                }
            }
        }

        val generatedResources = generateResources(resources)

        val generatedFiles = (generatedTypes + generatedResources).map { InMemoryGeneratedFile(it) }

        val existingFiles = File("/Users/mfudala/workspace/pulumi-kotlin/src/main/kotlin/com/virtuslab/pulumikotlin/codegen/sdk")
            .listFiles()
            .orEmpty()
            .map { ExistingFile("/Users/mfudala/workspace/pulumi-kotlin/src/main/kotlin", it.absolutePath, "com/pulumi/kotlin" + it.name) }

        return generatedFiles + existingFiles
    }
}
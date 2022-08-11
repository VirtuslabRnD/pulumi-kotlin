package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.CodeBlock
import com.virtuslab.pulumikotlin.codegen.step2intermediate.AutonomousType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic
import java.io.File

object Generate {
    fun generate(types: List<AutonomousType>, options: GenerationOptions = GenerationOptions()): List<WriteableFile> {
        val generatedTypes = types.filterIsInstance<ComplexType>().map { a ->
            when {
                a.metadata.useCharacteristic.toNested() == UseCharacteristic.FunctionNested || a.metadata.inputOrOutput == InputOrOutput.Output -> {
                    generateTypeWithNiceBuilders(
                        a.metadata,
                        a.fields.map { (name, type) ->
                            Field(
                                name,
                                NormalField(type) { from, to -> CodeBlock.of("val $to = $from") },
                                true,
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
                            Field(name, OutputWrappedField(type), true,
                                listOf(
                                    NormalField(type) { from, to -> CodeBlock.of("val $to = Output.of($from)") }
                                )
                            )
                        },
                        options
                    )
                }
            }
        }

        val generatedFiles = generatedTypes.map { InMemoryGeneratedFile(it) }

        val existingFiles = File("/Users/mfudala/workspace/pulumi-kotlin/src/main/kotlin/com/virtuslab/pulumikotlin/codegen/sdk")
            .listFiles()
            .orEmpty()
            .map { ExistingFile("/Users/mfudala/workspace/pulumi-kotlin/src/main/kotlin", it.absolutePath, "com/pulumi/kotlin" + it.name) }

        return generatedFiles + existingFiles
    }
}
package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.InputOrOutput
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PrimitiveType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeAndOptionality
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UseCharacteristic
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class CodeGeneratorGenerateFunctionsTest {

    @Test
    fun `generated kotlin files for some handcrafted functions should compile`() {
        val inputType = ComplexType(
            TypeMetadata(
                PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority"),
                InputOrOutput.Input,
                UseCharacteristic.FunctionRoot,
            ),

            mapOf(
                "arn" to TypeAndOptionality(PrimitiveType("String"), true),
            ),
        )
        val outputType = ComplexType(
            TypeMetadata(
                PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority"),
                InputOrOutput.Output,
                UseCharacteristic.FunctionRoot,
            ),

            mapOf(
                "arn" to TypeAndOptionality(PrimitiveType("String"), true),
            ),
        )
        val function = FunctionType(
            PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority"),
            inputType,
            outputType,
        )

        val generationOptions = TypeGenerator.GenerationOptions()
        val generatedFiles = CodeGenerator.run(
            GeneratorArguments(
                types = listOf(inputType, outputType),
                resources = emptyList(),
                functions = listOf(function),
                options = generationOptions,
            ),
        )

        val files = generatedFiles.map {
            it.get()
        }

        files.forEach { (_, contents) ->
            assertNotEquals("", contents.trim())
        }

        val sourceFiles = files.map { (name, contents) ->
            try {
                SourceFile.kotlin(name, contents)
            } catch (e: Exception) {
                println("Failure for $name: $contents")
                throw e
            }
        }

        val compileResult =
            KotlinCompilation()
                .apply {
                    sources = sourceFiles

                    classpaths = listOf(
                        artifact("com.pulumi:pulumi:0.5.2"),
                        artifact("com.pulumi:aws:5.11.0-alpha.1658776797+e45bda97"),
                        artifact("com.google.code.findbugs:jsr305:3.0.2"),
                        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.2"),
                    )

                    messageOutputStream = System.out
                }
                .compile()

        assertEquals(OK, compileResult.exitCode)
    }

    private fun artifact(coordinate: String) = ArtifactDownloader.download(coordinate).toFile()
}

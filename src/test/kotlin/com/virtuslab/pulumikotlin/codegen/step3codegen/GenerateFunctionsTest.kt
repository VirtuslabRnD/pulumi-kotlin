package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.FileSpec
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.*
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class GenerateFunctionsTest {

    @Test
    fun `generated kotlin files for some handcrafted functions should compile`() {
        val inputType = ComplexType(
            TypeMetadata(
                PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority"),
                InputOrOutput.Input,
                UseCharacteristic.FunctionRoot
            ),

            mapOf(
                "arn" to TypeAndOptionality(PrimitiveType("String"), true)
            )
        )
        val outputType = ComplexType(
            TypeMetadata(
                PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority"),
                InputOrOutput.Output,
                UseCharacteristic.FunctionRoot
            ),

            mapOf(
                "arn" to TypeAndOptionality(PrimitiveType("String"), true)
            )
        )
        val function = FunctionType(
            PulumiName.from("aws:acmpca/getCertificateAuthority:getCertificateAuthority"),
            inputType,
            outputType
        )

        val generationOptions = GenerationOptions()
        val generatedFiles = CodeGenerator.run(GeneratorArguments(
            types = listOf(inputType, outputType),
            resources = emptyList(),
            functions = listOf(function),
            options = generationOptions
        ))

        val files = generatedFiles.map {
            it.get()
        }

        files.forEach { (_, contents) ->
            assertNotEquals("", contents.trim())
        }

        val sourceFiles = files.map { (name, contents) ->
            try {
                SourceFile.kotlin(name, contents)
            } catch(e: Exception) {
                println("Failure for ${name}: ${contents}")
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
                        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.2")
                    )

                    messageOutputStream = System.out
                }
                .compile()


        assertEquals(OK, compileResult.exitCode)
    }

    private fun artifact(coordinate: String) = ArtifactDownloader.download(coordinate).toFile()

}

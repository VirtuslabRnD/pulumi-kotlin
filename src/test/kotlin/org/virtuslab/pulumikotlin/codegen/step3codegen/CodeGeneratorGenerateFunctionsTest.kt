@file:OptIn(ExperimentalCompilerApi::class)

package org.virtuslab.pulumikotlin.codegen.step3codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Root
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.FieldInfo
import org.virtuslab.pulumikotlin.codegen.step2intermediate.FunctionType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiName
import org.virtuslab.pulumikotlin.codegen.step2intermediate.StringType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import org.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import org.virtuslab.pulumikotlin.codegen.step2intermediate.UsageKind
import org.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator
import org.virtuslab.pulumikotlin.namingConfigurationWithSlashInModuleFormat
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class CodeGeneratorGenerateFunctionsTest {

    @Test
    fun `generated kotlin files for some handcrafted functions should compile`() {
        val inputType = ComplexType(
            TypeMetadata(
                PulumiName.from(
                    "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
                    namingConfigurationWithSlashInModuleFormat("aws"),
                ),
                UsageKind(Root, Function, Input),
                KDoc(null, null),
            ),

            mapOf(
                "arn" to FieldInfo(
                    StringType,
                    KDoc(null, null),
                ),
            ),
        )
        val outputType = ComplexType(
            TypeMetadata(
                PulumiName.from(
                    "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
                    namingConfigurationWithSlashInModuleFormat("aws"),
                ),
                UsageKind(Root, Function, Output),
                KDoc(null, null),
            ),

            mapOf(
                "arn" to FieldInfo(
                    StringType,
                    KDoc(null, null),
                ),
            ),
        )
        val function = FunctionType(
            PulumiName.from(
                "aws:acmpca/getCertificateAuthority:getCertificateAuthority",
                namingConfigurationWithSlashInModuleFormat("aws"),
            ),
            inputType,
            outputType.toReference(),
            KDoc(null, null),
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
            SourceFile.kotlin(name, contents)
        }

        val compileResult =
            KotlinCompilation()
                .apply {
                    sources = sourceFiles

                    classpaths = listOf(
                        artifact("org.virtuslab:pulumi-kotlin:0.11.0.0"),
                        artifact("com.pulumi:pulumi:0.11.0"),
                        artifact("com.pulumi:aws:6.37.1"),
                        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1"),
                        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1"),
                        artifact("com.google.code.gson:gson:2.11.0"),
                        artifact("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3"),
                        artifact("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.6.3"),
                    )

                    messageOutputStream = System.out

                    jvmTarget = JvmTarget.JVM_21.description
                }
                .compile()

        assertEquals(OK, compileResult.exitCode)
    }

    private fun artifact(coordinate: String) = ArtifactDownloader.download(coordinate).toFile()
}

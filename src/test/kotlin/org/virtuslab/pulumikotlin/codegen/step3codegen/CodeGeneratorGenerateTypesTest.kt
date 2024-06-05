@file:OptIn(ExperimentalCompilerApi::class)

package org.virtuslab.pulumikotlin.codegen.step3codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import org.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import org.virtuslab.pulumikotlin.codegen.step2intermediate.FieldInfo
import org.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiName
import org.virtuslab.pulumikotlin.codegen.step2intermediate.StringType
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import org.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import org.virtuslab.pulumikotlin.codegen.step2intermediate.UsageKind
import org.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class CodeGeneratorGenerateTypesTest {

    @Test
    fun `generated kotlin files for some handcrafted types should compile`() {
        val firstType = ComplexType(
            TypeMetadata(
                PulumiName("aws", null, emptyList(), null, "FirstType", false),
                UsageKind(Nested, Resource, Input),
                KDoc(null, null),
            ),

            mapOf(
                "field1" to FieldInfo(
                    StringType,
                    KDoc(null, null),
                ),
            ),
        )
        val secondType = ComplexType(
            TypeMetadata(
                PulumiName("aws", null, emptyList(), "aws", "SecondType", false),
                UsageKind(Nested, Resource, Input),
                KDoc(null, null),
            ),
            mapOf(
                "field2" to FieldInfo(
                    firstType.toReference(),
                    KDoc(null, null),
                ),
            ),
        )

        val generationOptions = TypeGenerator.GenerationOptions(implementToJava = false, implementToKotlin = false)
        val generatedFiles = CodeGenerator.run(
            GeneratorArguments(
                types = listOf(firstType, secondType),
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
                }
                .compile()

        assertEquals(OK, compileResult.exitCode)
    }

    private fun artifact(coordinate: String) = ArtifactDownloader.download(coordinate).toFile()
}

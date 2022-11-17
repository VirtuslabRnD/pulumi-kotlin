package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import com.virtuslab.pulumikotlin.codegen.step2intermediate.ComplexType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.PulumiName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.StringType
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeAndOptionality
import com.virtuslab.pulumikotlin.codegen.step2intermediate.TypeMetadata
import com.virtuslab.pulumikotlin.codegen.step2intermediate.UsageKind
import com.virtuslab.pulumikotlin.codegen.step3codegen.types.TypeGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class CodeGeneratorGenerateTypesTest {

    @Test
    fun `generated kotlin files for some handcrafted types should compile`() {
        val firstType = ComplexType(
            TypeMetadata(
                PulumiName("aws", listOf("aws"), "FirstType"),
                UsageKind(Nested, Resource, Input),
                KDoc(null, null),
            ),

            mapOf(
                "field1" to TypeAndOptionality(
                    StringType,
                    true,
                    KDoc(null, null),
                ),
            ),
        )
        val secondType = ComplexType(
            TypeMetadata(
                PulumiName("aws", listOf("aws"), "SecondType"),
                UsageKind(Nested, Resource, Input),
                KDoc(null, null),
            ),
            mapOf(
                "field2" to TypeAndOptionality(
                    firstType.toReference(),
                    true,
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
                        artifact("com.pulumi:pulumi:0.6.0"),
                        artifact("com.pulumi:aws:5.14.0"),
                        artifact("com.google.code.findbugs:jsr305:3.0.2"),
                        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4"),
                        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4"),
                        artifact("com.google.code.gson:gson:2.10"),
                        artifact("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.4.1"),
                        artifact("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.4.1"),
                    )

                    messageOutputStream = System.out
                }
                .compile()

        assertEquals(OK, compileResult.exitCode)
    }

    private fun artifact(coordinate: String) = ArtifactDownloader.download(coordinate).toFile()
}

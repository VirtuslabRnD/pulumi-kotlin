package com.virtuslab.pulumikotlin.codegen.step3codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.*
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.step2intermediate.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class GenerateTypeWithNiceBuildersTest {

    @Test
    fun `just test something`() {
        val firstType = TypeMetadata(
            PulumiName("aws", listOf("aws"), "FirstType"),
            InputOrOutput.Input,
            UseCharacteristic.ResourceNested
        )
        val secondType = TypeMetadata(
            PulumiName("aws", listOf("aws"), "SecondType"),
            InputOrOutput.Input,
            UseCharacteristic.ResourceNested
        )

        val generatedSpec1 = generateTypeWithNiceBuilders(
            firstType,
            listOf(
                Field(
                    "field1",
                    NormalField(PrimitiveType("String"), { from, to -> CodeBlock.of("val $to = $from") }),
                    required = true,
                    listOf()
                )
            )
        )
        val generatedSpec2 = generateTypeWithNiceBuilders(
            secondType,
            listOf(
                Field(
                    "field2",
                    NormalField(
                        ComplexType(
                            firstType,
                            mapOf(
                                "firstType" to PrimitiveType("String")
                            )
                        ),
                        { from, to -> CodeBlock.of("val $to = $from") }
                    ),
                    required = true,
                    listOf()
                )
            )
        )

        val compileResult =
            KotlinCompilation()
                .apply {
                    sources = listOf(generatedSpec1, generatedSpec2).map { fileSpecToSourceFile(it) }

                    messageOutputStream = System.out
                }
                .compile()


        assertEquals(OK, compileResult.exitCode)
    }

    private fun fileSpecToSourceFile(spec: FileSpec): SourceFile {
        return SourceFile.kotlin(
            spec.name,
            spec.toString()
        )
    }

}
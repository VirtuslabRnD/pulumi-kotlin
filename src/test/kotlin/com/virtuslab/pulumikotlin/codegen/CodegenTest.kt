package com.virtuslab.pulumikotlin.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.test.assertEquals

fun readFilesRecursively(directory: File): Map<String, String> {
    require(directory.isDirectory)

    return directory.listFiles()?.asSequence().orEmpty()
        .flatMap {
            if(it.isDirectory) {
                readFilesRecursively(it).map { (name, contents) -> name to contents }.asSequence()
            } else {
                sequenceOf(it.absolutePath to it.readText())
            }
        }
        .toMap()
}

class CodegenTest {

    @Test
    fun codegenTest() {
        val outputDirectory = Codegen.codegen(File("/Users/mfudala/workspace/pulumi-kotlin/src/test/resources/test-schema.json"))

        val generatedKotlinFiles = readFilesRecursively(outputDirectory).map { (fileName, contents) -> SourceFile.kotlin(fileName, contents) }

        val exampleFile = SourceFile.new("Main.kt", """
            import com.pulumi.aws.acmpca.kotlin.inputs.GetCertificateAuthorityRevocationConfigurationArgsBuilder
            
            suspend fun main() {
                val builder = GetCertificateAuthorityRevocationConfigurationArgsBuilder()
                
                with(builder) {
                    crlConfigurations(
                        { 
                            customCname("whatever")
                            enabled(true)
                            expirationInDays(5)
                        },
                        {
                            customCname("otherCname")
                        }
                    )
                }
            }
        """.trimIndent())


        val compilation = KotlinCompilation().apply {
            sources = listOf(exampleFile) + generatedKotlinFiles

            classpaths = listOf(
                artifact("com.pulumi:pulumi:0.5.2"),
                artifact("com.pulumi:aws:5.11.0-alpha.1658776797+e45bda97"),
                artifact("com.google.code.findbugs:jsr305:3.0.2")
            )
            messageOutputStream = System.out
        }

        assertEquals(compilation.compile().exitCode, KotlinCompilation.ExitCode.OK)
    }

    private fun artifact(coordinate: String) = ArtifactDownloader.download(coordinate).toFile()
}
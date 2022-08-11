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

    private val classPath = listOf(
        artifact("com.pulumi:pulumi:0.5.2"),
        artifact("com.pulumi:aws:5.11.0-alpha.1658776797+e45bda97"),
        artifact("com.google.code.findbugs:jsr305:3.0.2")
    )

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

            classpaths = classPath
            messageOutputStream = System.out
        }

        assertEquals(KotlinCompilation.ExitCode.OK, compilation.compile().exitCode)
    }

    @Test
    fun codegenTestWithTypesDerivedFromFunctions() {
        val outputDirectory = Codegen.codegen(File("/Users/mfudala/workspace/pulumi-kotlin/src/test/resources/test-schema.json"))

        println(outputDirectory)

        val generatedKotlinFiles = readFilesRecursively(outputDirectory).map { (fileName, contents) -> SourceFile.kotlin(fileName, contents) }

        val exampleFile = SourceFile.new("Main.kt", """
            import com.pulumi.aws.acmpca.kotlin.inputs.GetCertificateAuthorityArgsBuilder
            
            suspend fun main() {
                val builder = GetCertificateAuthorityArgsBuilder()
                
                with(builder) {
                    revocationConfigurations(
                        {
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
                    )
                }
            }
        """.trimIndent())


        val compilation = KotlinCompilation().apply {
            sources = listOf(exampleFile) + generatedKotlinFiles

            classpaths = classPath
            messageOutputStream = System.out
        }

        assertEquals(KotlinCompilation.ExitCode.OK, compilation.compile().exitCode)
    }

    @Test
    fun codegenTestWithTypesDerivedFromResources() {
        val outputDirectory = Codegen.codegen(File("/Users/mfudala/workspace/pulumi-kotlin/src/test/resources/test-schema.json"))

        println(outputDirectory)

        val generatedKotlinFiles = readFilesRecursively(outputDirectory).map { (fileName, contents) -> SourceFile.kotlin(fileName, contents) }

        val exampleFile = SourceFile.new("Main.kt", """
            import com.pulumi.aws.acm.kotlin.CertificateArgsBuilder
            
            suspend fun main() {
                val builder = CertificateArgsBuilder()
                
                with(builder) {
                    domainName("whatever")
                    options {
                        certificateTransparencyLoggingPreference("Omg")
                    }
                    subjectAlternativeNames("one", "two")
                }
            }
        """.trimIndent())


        val compilation = KotlinCompilation().apply {
            sources = listOf(exampleFile) + generatedKotlinFiles

            classpaths = classPath
            messageOutputStream = System.out
        }

        assertEquals(KotlinCompilation.ExitCode.OK, compilation.compile().exitCode)
    }

    private fun artifact(coordinate: String) = ArtifactDownloader.download(coordinate).toFile()
}
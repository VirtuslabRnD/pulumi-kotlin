package com.virtuslab.pulumikotlin.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
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
        testCompilationWithSourceFiles("test-schema.json", mapOf("Main.kt" to """
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
        """))
    }

    @Test
    fun codegenTestWithTypesDerivedFromFunctions() {
        testCompilationWithSourceFiles("test-schema.json", mapOf("Main.kt" to """
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
        """))
    }

    @Test
    fun codegenTestWithTypesDerivedFromResources() {
        testCompilationWithSourceFiles("test-schema.json", mapOf("Main.kt" to """
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
        """)
        )
    }

    @Test
    fun codegenTestWholeResourceCreationWithoutOutputs() {
       testCompilationWithSourceFiles(
           "test-schema.json", mapOf("Main.kt" to """
            import com.pulumi.aws.acm.kotlin.certificateResource
            
            suspend fun main() {
                certificateResource("name") {
                    args {
                        subjectAlternativeNames("one", "two")
                        validationOptions(
                            {
                                domainName("whatever")
                                validationDomain("whatever")
                            },
                            {
                                domainName("whatever2")
                                validationDomain("whatever2")
                            }
                        )
                        options {
                            certificateTransparencyLoggingPreference("test")
                        }
                    }
                    opts {
                        protect(true)
                        retainOnDelete(false)
                        ignoreChanges(listOf("asd"))
                    }
                }
            }
        """)
       )
    }

    @Test
    fun `resource can be created and its outputs can be used elsewhere`() {
        testCompilationWithSourceFiles("test-schema.json", mapOf(
            "Main.kt" to """
            import com.pulumi.aws.acm.kotlin.certificateResource
            
            suspend fun main() {
                val resource1 = certificateResource("name") {
                    args {
                        subjectAlternativeNames("one", "two")
                        validationOptions(
                            {
                                domainName("whatever")
                                validationDomain("whatever")
                            },
                            {
                                domainName("whatever2")
                                validationDomain("whatever2")
                            }
                        )
                        options {
                            certificateTransparencyLoggingPreference("test")
                        }
                    }
                    opts {
                        protect(true)
                        retainOnDelete(false)
                        ignoreChanges(listOf("asd"))
                    }
                }

                val resource2 = certificateResource("name") {
                    args {
                        subjectAlternativeNames(resource1.status.applyValue { listOf(it) })
                        validationOptions(
                            {
                                domainName(resource1.status)
                                validationDomain("whatever")
                            }
                        )
                        options {
                            certificateTransparencyLoggingPreference("test")
                        }
                    }
                    opts {
                        protect(true)
                        retainOnDelete(false)
                        ignoreChanges(listOf("asd"))
                    }
                }
            }
        """
        ))
    }

    @Test
    fun `functions can be invoked`() {
        testCompilationWithSourceFiles("test-schema.json", mapOf(
            "Main.kt" to """
            import com.pulumi.aws.acmpca.kotlin.AcmpcaFunctions.getCertificateAuthority
            
            suspend fun main() {
                val cert = getCertificateAuthority(arn = "www.wp.pl", tags = mapOf("a" to "b"))

                cert.arn
            }
        """
        ))
    }

    @Test
    fun `functions can be invoked type-safe builder style`() {
        testCompilationWithSourceFiles("test-schema.json", mapOf(
            "Main.kt" to """
            import com.pulumi.aws.acmpca.kotlin.AcmpcaFunctions.getCertificateAuthority
            
            suspend fun main() {
                val cert = getCertificateAuthority {
                    arn("www.wp.pl")
                    revocationConfigurations({
                       crlConfigurations(
                            { 
                                customCname("firstCname")
                                enabled(true)
                            },
                            { 
                                customCname("otherCname")
                                enabled(false)
                            }
                       )
                    })
                    tags("a" to "b")
                }

                cert.arn
            }
        """
        ))
    }


    @Tag("slow")
    @Test
    fun codegenTestWholeAwsClassicSchema() {
        testCompilationWithSourceFiles("test-schema-bigger.json", emptyMap())
    }


    private val classPath = listOf(
        artifact("com.pulumi:pulumi:0.5.2"),
        artifact("com.pulumi:aws:5.11.0-alpha.1658776797+e45bda97"),
        artifact("com.google.code.findbugs:jsr305:3.0.2"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.2")
    )

    private fun testCompilationWithSourceFiles(schemaPath: String, sourceFiles: Map<String, String>) {

        val outputDirectory = Codegen.codegen(loadResource("/$schemaPath"))

        println(outputDirectory)

        val generatedKotlinFiles = readFilesRecursively(outputDirectory).map { (fileName, contents) -> SourceFile.kotlin(fileName, contents) }

        val hardcodedSources = sourceFiles.map { (fileName, source) ->
            SourceFile.new(fileName, source.trimIndent())
        }

        val compilation = KotlinCompilation().apply {
            sources = hardcodedSources + generatedKotlinFiles

            classpaths = classPath
            messageOutputStream = System.out
        }

        assertEquals(KotlinCompilation.ExitCode.OK, compilation.compile().exitCode)
    }

    private fun artifact(coordinate: String) =
        ArtifactDownloader.download(coordinate).toFile()

    private fun loadResource(path: String) =
        CodegenTest::class.java.getResourceAsStream(path) ?: error("$path does not exist")
}

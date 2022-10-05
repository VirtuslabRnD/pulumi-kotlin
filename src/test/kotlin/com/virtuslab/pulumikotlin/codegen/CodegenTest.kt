package com.virtuslab.pulumikotlin.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.INTERNAL_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.SCRIPT_EXECUTION_ERROR
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class CodegenTest {
    @Test
    fun `aws resource can be created`() {
        // language=kotlin
        val code = """
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
           """

        assertGeneratedCodeAndSourceFileCompile(AWS_SMALL_SCHEMA_SUBSET, code)
    }

    @Test
    fun `aws resource can be created and its outputs can be used to create another aws resource`() {
        // language=kotlin
        val code = """
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

        assertGeneratedCodeAndSourceFileCompile(AWS_SMALL_SCHEMA_SUBSET, code)
    }

    @Test
    fun `aws functions can be invoked`() {
        // language=kotlin
        val code = """
            import com.pulumi.aws.acmpca.kotlin.AcmpcaFunctions.getCertificateAuthority
            
            suspend fun main() {
                val cert = getCertificateAuthority(arn = "www.wp.pl", tags = mapOf("a" to "b"))

                cert.arn
            }
            """

        assertGeneratedCodeAndSourceFileCompile(AWS_SMALL_SCHEMA_SUBSET, code)
    }

    @Test
    fun `aws functions can be invoked, type-safe builder variation`() {
        // language=kotlin
        val code = """
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

        assertGeneratedCodeAndSourceFileCompile(AWS_SMALL_SCHEMA_SUBSET, code)
    }

    @Test
    fun `aws methods using Either and Enum can be invoked`() {
        // language=kotlin
        val code = """
            import com.pulumi.aws.route53.kotlin.enums.RecordType
            import com.pulumi.aws.route53.kotlin.recordResource
            import com.pulumi.core.Either

            private const val RECORD_NAME = "record"
            private const val ZONE_ID = "zoneId"
            
            suspend fun main() {
                val record = recordResource(RECORD_NAME) {
                    name(RECORD_NAME)
                    args {
                        aliases(
                            {
                                evaluateTargetHealth(true)
                                name("name")
                                zoneId(ZONE_ID)
                            }
                        )
                        allowOverwrite(true)
                        failoverRoutingPolicies(
                            {
                                type("type")
                            }
                        )
                        geolocationRoutingPolicies(
                            {
                                continent("continent")
                                country("country")
                                subdivision("subdivision")
                            }
                        )
                        healthCheckId("healthCheckId")
                        latencyRoutingPolicies(
                            {
                                region("region")
                            }
                        )
                        multivalueAnswerRoutingPolicy(true)
                        name(RECORD_NAME)
                        records("records")
                        setIdentifier("setIdentifier")
                        ttl(1)
                        type(Either.ofRight(RecordType.AAAA))
                        weightedRoutingPolicies(
                            {
                                weight(1)
                            }
                        )
                        zoneId(ZONE_ID)
                    }
                }
            
                record.type
            }

            """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_WITH_ONE_OF, code)
    }

    @Test
    fun `bigger subset of aws schema can be compiled`() {
        assertGeneratedCodeCompiles(AWS_BIG_SCHEMA_SUBSET)
    }

    @Test
    fun `test medium-sized google cloud schema (without asset or archive types)`() {
        // language=kotlin
        val code = """
            import com.pulumi.gcp.appengine.kotlin.applicationUrlDispatchRulesResource
            
            suspend fun main() {
                applicationUrlDispatchRulesResource("resource-name") {
                    args {
                        project("example-project")
                        dispatchRules(
                            {
                                domain("domain")
                                path("path")
                            },
                            {
                                domain("domain2")
                                path("path2")
                            }
                        )
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(GCP_MEDIUM_SCHEMA_SUBSET, code)
    }

    @Test
    fun `type-safe resource builder cannot be directly constructed`() {
        // language=kotlin
        val code = """
            import com.pulumi.gcp.appengine.kotlin.ApplicationUrlDispatchRulesResourceBuilder
            
            suspend fun main() {
                ApplicationUrlDispatchRulesResourceBuilder()
            }
        """

        assertGeneratedCodeAndSourceFileDoNotCompile(GCP_MEDIUM_SCHEMA_SUBSET, code)
    }

    @Test
    fun `build from type-safe resource builder cannot be called directly`() {
        // language=kotlin
        val code = """
            import com.pulumi.gcp.appengine.kotlin.ApplicationUrlDispatchRulesResourceBuilder
            
            suspend fun main() {
                val builder: ApplicationUrlDispatchRulesResourceBuilder = null!!
                builder.build()
            }
        """

        assertGeneratedCodeAndSourceFileDoNotCompile(GCP_MEDIUM_SCHEMA_SUBSET, code)
    }

    @Test
    fun `type-safe type builder cannot be directly constructed`() {
        // language=kotlin
        val code = """
            import com.pulumi.gcp.appengine.kotlin.inputs.ApplicationUrlDispatchRulesDispatchRuleArgsBuilder
            
            suspend fun main() {
                ApplicationUrlDispatchRulesDispatchRuleArgsBuilder()
            }
        """

        assertGeneratedCodeAndSourceFileDoNotCompile(GCP_MEDIUM_SCHEMA_SUBSET, code)
    }

    @Test
    fun `build from type-safe type builder cannot be called directly`() {
        // language=kotlin
        val code = """
            import com.pulumi.gcp.appengine.kotlin.inputs.ApplicationUrlDispatchRulesDispatchRuleArgsBuilder
            
            suspend fun main() {
                val builder: ApplicationUrlDispatchRulesDispatchRuleArgsBuilder = null!!
                builder.build()
            }
        """

        assertGeneratedCodeAndSourceFileDoNotCompile(GCP_MEDIUM_SCHEMA_SUBSET, code)
    }

    @Test
    fun `nested type-safe builder should not allow parent type-safe builder's method calls`() {
        // language=kotlin
        val code = """
            import com.pulumi.gcp.appengine.kotlin.applicationUrlDispatchRulesResource
            
            suspend fun main() {
                applicationUrlDispatchRulesResource("resource-name") {
                    args {
                        project("example-project")
                        dispatchRules(
                            {
                                project("THIS-SHOULD-NOT-WORK")
                                domain("domain")
                                path("path")
                            },
                            {
                                domain("domain2")
                                path("path2")
                            }
                        )
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileDoNotCompile(GCP_MEDIUM_SCHEMA_SUBSET, code)
    }

    private val classPath = listOf(
        artifact("com.pulumi:pulumi:0.6.0"),
        artifact("com.pulumi:aws:5.14.0"),
        artifact("com.pulumi:gcp:6.37.0"),
        artifact("com.google.code.findbugs:jsr305:3.0.2"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4"),
    )

    private fun assertGeneratedCodeCompiles(schemaPath: String) {
        assertGeneratedCodeAndSourceFilesCompile(schemaPath, emptyMap())
    }

    private fun assertGeneratedCodeAndSourceFileCompile(schemaPath: String, sourceFile: String) {
        assertGeneratedCodeAndSourceFilesCompile(schemaPath, mapOf("Main.kt" to sourceFile))
    }

    private fun assertGeneratedCodeAndSourceFileDoNotCompile(schemaPath: String, sourceFile: String) {
        assertGeneratedCodeAndSourceFilesDoNotCompile(schemaPath, mapOf("Main.kt" to sourceFile))
    }

    private fun assertGeneratedCodeAndSourceFilesDoNotCompile(schemaPath: String, sourceFiles: Map<String, String>) {
        val compilation = generateCodeAndCompileAsSeparateModules(schemaPath, sourceFiles)
        val compilationResult = compilation.compile()

        if (compilationResult.exitCode == COMPILATION_ERROR) {
            println("Code did not compile (as expected). Encountered problems: ${compilationResult.messages}")
        }
        assertEquals(
            COMPILATION_ERROR,
            compilationResult.exitCode,
            "Code did compile (not expected)",
        )
    }

    private fun assertGeneratedCodeAndSourceFilesCompile(schemaPath: String, sourceFiles: Map<String, String>) {
        val compilation = generateCodeAndCompileAsSeparateModules(schemaPath, sourceFiles)
        val compilationResult = compilation.compile()

        assertEquals(
            OK,
            compilationResult.exitCode,
            "Code did not compile (not expected). Encountered problems ${compilationResult.messages}",
        )
    }

    data class AggregateCompilationResult(val exitCode: KotlinCompilation.ExitCode, val messages: String) {
        companion object {
            fun from(perModuleResult: Map<String, KotlinCompilation.Result>): AggregateCompilationResult {
                require(perModuleResult.isNotEmpty()) { "Should have at least 1 element" }

                val fromWorstToBest = listOf(SCRIPT_EXECUTION_ERROR, INTERNAL_ERROR, COMPILATION_ERROR, OK)

                val worstExitCode = fromWorstToBest
                    .find { possibleExitCode ->
                        perModuleResult.values.any { actualResult ->
                            actualResult.exitCode == possibleExitCode
                        }
                    }
                    ?: error("unexpected")

                val concatenatedMessages = perModuleResult
                    .flatMap { (moduleName, result) ->
                        result.messages.lines().map { line ->
                            "[module: $moduleName] $line"
                        }
                    }
                    .joinToString(",")

                return AggregateCompilationResult(worstExitCode, concatenatedMessages)
            }
        }
    }

    /**
     * Simulate modules, so that proper encapsulation can be tested (`internal` visibility modifier).
     *
     * This compiles files/directories, that would normally live in different artifacts, separately.
     *
     * [Kotlin docs](https://kotlinlang.org/docs/visibility-modifiers.html#modules):
     *
     * A module is a set of Kotlin files compiled together, for example:
     * - ...
     * - **A set of files compiled with one invocation of the <kotlinc> Ant task.**
     */
    data class ModularizedKotlinCompilation(val compilationPerModule: Map<String, KotlinCompilation>) {
        fun compile(): AggregateCompilationResult {
            return AggregateCompilationResult.from(
                compilationPerModule.mapValues { (_, value) -> value.compile() },
            )
        }
    }

    private fun generateCodeAndCompileAsSeparateModules(
        schemaPath: String,
        sourceFiles: Map<String, String>,
    ): ModularizedKotlinCompilation {
        val outputDirectory = Codegen.codegen(loadResource("/$schemaPath"))
        val generatedKotlinFiles = readFilesRecursively(outputDirectory)
            .map { (fileName, contents) -> SourceFile.kotlin(fileName, contents) }

        val hardcodedSources = sourceFiles
            .map { (fileName, source) -> SourceFile.new(fileName, source.trimIndent()) }

        val compilation = KotlinCompilation().apply {
            sources = generatedKotlinFiles
            classpaths = classPath
        }

        val compilationTwo = KotlinCompilation().apply {
            sources = hardcodedSources
            classpaths = classPath + compilation.classesDir
        }

        return ModularizedKotlinCompilation(mapOf("fromSchema" to compilation, "main" to compilationTwo))
    }

    private fun artifact(coordinate: String) =
        ArtifactDownloader.download(coordinate).toFile()

    private fun loadResource(path: String) =
        CodegenTest::class.java.getResourceAsStream(path) ?: error("$path does not exist")

    private fun readFilesRecursively(directory: File): Map<String, String> {
        require(directory.isDirectory)

        return directory.listFiles()?.asSequence().orEmpty()
            .flatMap {
                if (it.isDirectory) {
                    readFilesRecursively(it).map { (name, contents) -> name to contents }.asSequence()
                } else {
                    sequenceOf(it.absolutePath to it.readText())
                }
            }
            .toMap()
    }
}

private const val GCP_MEDIUM_SCHEMA_SUBSET = "schema-gcp-classic-subset-medium-size.json"
private const val AWS_SMALL_SCHEMA_SUBSET = "schema-aws-classic-subset-small-size.json"
private const val AWS_BIG_SCHEMA_SUBSET = "schema-aws-classic-subset-big-size.json"
private const val SCHEMA_AWS_CLASSIC_SUBSET_WITH_ONE_OF = "schema-aws-classic-5.15.0-subset-with-one-of.json"

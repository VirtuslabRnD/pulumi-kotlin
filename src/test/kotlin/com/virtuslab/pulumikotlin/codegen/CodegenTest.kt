package com.virtuslab.pulumikotlin.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.INTERNAL_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.SCRIPT_EXECUTION_ERROR
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.System.lineSeparator
import kotlin.test.assertEquals

class CodegenTest {

    private val logger = KotlinLogging.logger {}

    private val classPath = listOf(
        artifact("com.pulumi:pulumi:0.6.0"),
        artifact("com.pulumi:aws:5.16.2"),
        artifact("com.pulumi:gcp:6.38.0"),
        artifact("com.pulumi:slack:0.3.0"),
        artifact("com.google.code.findbugs:jsr305:3.0.2"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4"),
    )

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

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_SMALL_SIZE, code)
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

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_SMALL_SIZE, code)
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

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_SMALL_SIZE, code)
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

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_SMALL_SIZE, code)
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
        assertGeneratedCodeCompiles(SCHEMA_AWS_CLASSIC_SUBSET_BIG_SIZE)
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

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_GCP_CLASSIC_SUBSET_MEDIUM_SIZE, code)
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

        assertGeneratedCodeAndSourceFileDoNotCompile(SCHEMA_GCP_CLASSIC_SUBSET_MEDIUM_SIZE, code)
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

        assertGeneratedCodeAndSourceFileDoNotCompile(SCHEMA_GCP_CLASSIC_SUBSET_MEDIUM_SIZE, code)
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

        assertGeneratedCodeAndSourceFileDoNotCompile(SCHEMA_GCP_CLASSIC_SUBSET_MEDIUM_SIZE, code)
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

        assertGeneratedCodeAndSourceFileDoNotCompile(SCHEMA_GCP_CLASSIC_SUBSET_MEDIUM_SIZE, code)
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
                                path("path")
                            },
                        )
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileDoNotCompile(SCHEMA_GCP_CLASSIC_SUBSET_MEDIUM_SIZE, code)
    }

    @Test
    fun `aws functions from index namespace can be invoked`() {
        // language=kotlin
        val code = """
            import com.pulumi.aws.kotlin.AwsFunctions

            suspend fun main() {
                AwsFunctions.getAmi {
                    nameRegex("ami.*")
                }
            }
            """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_WITH_INDEX, code)
    }

    @Test
    fun `slack resources from index namespace can be created`() {
        // language=kotlin
        val code = """
            import com.pulumi.slack.kotlin.conversationResource

            suspend fun main() {
                conversationResource("conversationName") {
                    args {
                        topic("conversationTopic")
                    }
                    opts {
                        protect(true)
                    }
                }
            }
            """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_SLACK_SUBSET_WITH_INDEX, code)
    }

    @Test
    fun `google cloud lb ip ranges function (zero args)`() {
        assertGeneratedCodeCompiles(SCHEMA_GCP_CLASSIC_SUBSET_LB_IP_RANGES)
    }

    @Test
    fun `aws function with with pulumi(dot)json#(slash)Asset argument can be invoked with an archive argument`() {
        // language=kotlin
        val code = """
            import com.pulumi.asset.AssetArchive
            import com.pulumi.asset.FileArchive
            import com.pulumi.asset.RemoteArchive
            import com.pulumi.asset.StringAsset
            import com.pulumi.aws.lambda.kotlin.functionResource

            suspend fun main() {
                functionResource("function") {
                    args {
                        code(
                            AssetArchive(
                                mapOf(
                                    "string" to StringAsset("Hello, world!"),
                                    "file" to FileArchive("./folder"),
                                    "remote" to RemoteArchive("https://example.com/file.zip"),
                                ),
                            ),
                        )
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_WITH_ARCHIVE, code)
    }

    @Test
    fun `aws function with with pulumi(dot)json#(slash)Archive argument can be invoked with an asset argument`() {
        // language=kotlin
        val code = """
            import com.pulumi.asset.StringAsset
            import com.pulumi.aws.s3.kotlin.bucketObjectResource   

            suspend fun main() {
                bucketObjectResource("bucketObjectResource") {
                    args {
                        source(StringAsset("Hello world!"))
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_WITH_ASSET, code)
    }

    @Test
    fun `aws function with with pulumi(dot)json#(slash)Archive argument can be invoked with an archive argument`() {
        // language=kotlin
        val code = """
            import com.pulumi.asset.FileArchive
            import com.pulumi.aws.s3.kotlin.bucketObjectResource   

            suspend fun main() {
                bucketObjectResource("bucketObjectResource") {
                    args {
                        source(FileArchive("./folder"))
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_WITH_ASSET, code)
    }

    @Test
    fun `IpAllocationMethod (azure-native) should work despite being defined two times (with different casing)`() {
        assertGeneratedCodeCompiles(SCHEMA_AZURE_NATIVE_SUBSET_WITH_IP_ALLOCATION)
    }

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
        val compilationResult = generateCodeAndCompileAsSeparateModules(schemaPath, sourceFiles)

        if (compilationResult.exitCode == COMPILATION_ERROR) {
            logger.info("Code did not compile (as expected). Encountered problems:\n${compilationResult.messages}")
        }
        assertEquals(
            COMPILATION_ERROR,
            compilationResult.exitCode,
            "Code did compile (not expected)",
        )
    }

    private fun assertGeneratedCodeAndSourceFilesCompile(schemaPath: String, sourceFiles: Map<String, String>) {
        val compilationResult = generateCodeAndCompileAsSeparateModules(schemaPath, sourceFiles)

        assertEquals(
            OK,
            compilationResult.exitCode,
            "Code did not compile (not expected). Encountered problems ${compilationResult.messages}",
        )
    }

    data class AggregateCompilationResult(val exitCode: KotlinCompilation.ExitCode, val messages: String) {
        companion object {
            fun from(perModuleResult: Map<String, KotlinCompilation.Result>): AggregateCompilationResult {
                require(perModuleResult.isNotEmpty()) { "Expected perModuleResult to have at least 1 element" }

                val exitCodesFromWorstToBest = listOf(SCRIPT_EXECUTION_ERROR, INTERNAL_ERROR, COMPILATION_ERROR, OK)
                val receivedExitCodes = perModuleResult.values.map { it.exitCode }
                val worstExitCode = exitCodesFromWorstToBest
                    .find { possibleExitCode -> receivedExitCodes.contains(possibleExitCode) }
                    ?: error("Unexpected ($receivedExitCodes, $exitCodesFromWorstToBest)")

                val concatenatedMessages = perModuleResult
                    .flatMap { (moduleName, result) ->
                        result.messages.lines().map { line ->
                            "[module: $moduleName] $line"
                        }
                    }
                    .joinToString(lineSeparator())

                return AggregateCompilationResult(worstExitCode, concatenatedMessages)
            }
        }
    }

    /**
     * This simulates modules (generated code being a separate module from code provided by the user),
     * so that proper encapsulation can be tested (`internal` visibility modifier).
     *
     * How? This compiles files/directories, that would normally live in different artifacts, separately.
     *
     * [Kotlin docs](https://kotlinlang.org/docs/visibility-modifiers.html#modules):
     *
     * A module is a set of Kotlin files compiled together, for example:
     * - ...
     * - **A set of files compiled with one invocation of the <kotlinc> Ant task.**
     *
     */
    private fun generateCodeAndCompileAsSeparateModules(
        schemaPath: String,
        sourceFiles: Map<String, String>,
    ): AggregateCompilationResult {
        val outputDirectory = Codegen.codegen(loadResource("/$schemaPath"))
        val generatedSourceFiles = readFilesRecursively(outputDirectory)
            .map { (fileName, contents) -> SourceFile.kotlin(fileName, contents) }

        val additionalSourceFiles = sourceFiles
            .map { (fileName, source) -> SourceFile.new(fileName, source.trimIndent()) }

        val compilationForGeneratedCode = KotlinCompilation().apply {
            sources = generatedSourceFiles
            classpaths = classPath
        }

        val compilationForAdditionalCode = KotlinCompilation().apply {
            sources = additionalSourceFiles
            classpaths = classPath + compilationForGeneratedCode.classesDir
        }

        val compiledGeneratedCode = compilationForGeneratedCode.compile()
        val compiledAdditionalCode = compilationForAdditionalCode.compile()

        return AggregateCompilationResult.from(
            mapOf(
                "generatedCode" to compiledGeneratedCode,
                "additionalCode" to compiledAdditionalCode,
            ),
        )
    }

    private fun artifact(coordinate: String) =
        ArtifactDownloader.download(coordinate).toFile()

    private fun loadResource(path: String) =
        CodegenTest::class.java.getResourceAsStream(path) ?: error("Resource under path $path does not exist")

    private fun readFilesRecursively(directory: File): Map<String, String> {
        require(directory.isDirectory) { "Only directories are allowed" }

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

private const val SCHEMA_GCP_CLASSIC_SUBSET_MEDIUM_SIZE = "schema-gcp-classic-subset-medium-size.json"
private const val SCHEMA_GCP_CLASSIC_SUBSET_LB_IP_RANGES = "schema-gcp-classic-6.39.0-subset-lb-ip-ranges.json"
private const val SCHEMA_AWS_CLASSIC_SUBSET_SMALL_SIZE = "schema-aws-classic-subset-small-size.json"
private const val SCHEMA_AWS_CLASSIC_SUBSET_BIG_SIZE = "schema-aws-classic-subset-big-size.json"
private const val SCHEMA_AWS_CLASSIC_SUBSET_WITH_ONE_OF = "schema-aws-classic-5.15.0-subset-with-one-of.json"
private const val SCHEMA_AWS_CLASSIC_SUBSET_WITH_ARCHIVE = "schema-aws-classic-5.16.2-subset-with-archive.json"
private const val SCHEMA_AWS_CLASSIC_SUBSET_WITH_ASSET = "schema-aws-classic-5.16.2-subset-with-asset.json"
private const val SCHEMA_AWS_CLASSIC_SUBSET_WITH_INDEX = "schema-aws-classic-5.15.2-subset-with-index.json"
private const val SCHEMA_SLACK_SUBSET_WITH_INDEX = "schema-slack-0.3.0-subset-with-index.json"
private const val SCHEMA_AZURE_NATIVE_SUBSET_WITH_IP_ALLOCATION =
    "schema-azure-native-3.44.2-subset-with-ip-allocation.json"

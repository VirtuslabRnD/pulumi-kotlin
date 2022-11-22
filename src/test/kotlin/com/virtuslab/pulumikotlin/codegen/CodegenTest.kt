@file:Suppress("MoveLambdaOutsideParentheses")

package com.virtuslab.pulumikotlin.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.INTERNAL_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.SCRIPT_EXECUTION_ERROR
import com.tschuchort.compiletesting.SourceFile
import com.virtuslab.pulumikotlin.codegen.maven.ArtifactDownloader
import com.virtuslab.pulumikotlin.ejectToMavenProject
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.lang.System.lineSeparator
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals

class CodegenTest {

    private val logger = KotlinLogging.logger {}

    private val dependencies = listOf(
        "com.pulumi:pulumi:0.6.0",
        "com.pulumi:aws:5.16.2",
        "com.pulumi:gcp:6.38.0",
        "com.pulumi:slack:0.3.0",
        "com.pulumi:github:4.17.0",
        "com.pulumi:google-native:0.27.0",
        "com.pulumi:kubernetes:3.22.1",
        "com.pulumi:azure-native:1.85.0",
        "com.google.code.findbugs:jsr305:3.0.2",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4",
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4",
        "com.google.code.gson:gson:2.10",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.4.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.4.1",
    )

    private lateinit var testInfo: TestInfo

    @BeforeEach
    fun setup(testInfo: TestInfo) {
        this.testInfo = testInfo
    }

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
    fun `aws provider resource can be created`() {
        // language=kotlin
        val code = """
            import com.pulumi.aws.acm.kotlin.certificateResource
            import com.pulumi.aws.kotlin.providerResource
            
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
                        provider(
                            providerResource("custom-aws-provider") {
                                args {
                                    accessKey("123")
                                    allowedAccountIds("1", "2")
                                    assumeRole {
                                        duration("10")
                                        policy("centrally planned economy")
                                    }
                                    assumeRoleWithWebIdentity {
                                        roleArn("roleArm")
                                    }
                                    customCaBundle("can bundle")
                                    ec2MetadataServiceEndpoint("ec2MetadataServiceEndpoint")
                                    ec2MetadataServiceEndpointMode("ec2MetadataServiceEndpointMode")
                                    endpoints({
                                        acm("acm")
                                    })
                                    forbiddenAccountIds("1", "2")
                                    httpProxy("127.0.0.1")
                                    ignoreTags {
                                        keys("tagKey")
                                    }
                                    insecure(false)
                                    maxRetries(5)
                                    profile("dev")
                                    region("EUCentral1")
                                }
                            }
                        )
                    }
                }
            }
           """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AWS_CLASSIC_SUBSET_WITH_PROVIDER_AND_CERTIFICATE, code)
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

    @Test
    fun `GitHub code with colliding names gets generated correctly`() {
        // language=kotlin
        val code = """
            import com.pulumi.github.kotlin.GithubFunctions

            suspend fun main() {
                val getRepositoryPullRequestsInvokeResult = GithubFunctions.getRepositoryPullRequests {
                    baseRepository("whatever")
                }
        
                println(getRepositoryPullRequestsInvokeResult.results)
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_GITHUB_SUBSET_WITH_NAME_COLLISION, code)
    }

    @Test
    fun `code generated from schema with invalid token can be compiled`() {
        // The function `google-native:container/v1:Cluster/getKubeconfig` is skipped from code generation
        assertGeneratedCodeCompiles(SCHEMA_GOOGLE_NATIVE_SUBSET_WITH_INVALID_NAME)
    }

    @Test
    fun `code generated from snippet that contains a type property with keyword name (object) can be compiled`() {
        // language=kotlin
        val code = """
            import com.pulumi.googlenative.cloudbuild.v1.inputs.StorageSourceArgs
            
            suspend fun main() { 
                StorageSourceArgs.builder()
                    .`object`("object")
                    .build()
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_GOOGLE_NATIVE_SUBSET_WITH_KEYWORD_TYPE_PROPERTY, code)
    }

    @Test
    fun `functions with namespaces that contain a slash get mapped correctly`() {
        // language=kotlin
        val code = """
            import com.pulumi.googlenative.accesscontextmanager.v1.kotlin.Accesscontextmanager_v1Functions
            
            suspend fun main() {
                Accesscontextmanager_v1Functions.getAccessLevel { 
                    accessLevelFormat("format")
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_GOOGLE_NATIVE_SUBSET_NAMESPACE_WITH_SLASH, code)
    }

    @Test
    fun `types with no properties are generated correctly (not as data classes)`() {
        // language=kotlin
        val code = """
            import com.pulumi.googlenative.cloudchannel.v1.kotlin.inputs.GoogleCloudChannelV1RepricingConfigChannelPartnerGranularityArgs

            suspend fun main() {
                GoogleCloudChannelV1RepricingConfigChannelPartnerGranularityArgs()
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_GOOGLE_NATIVE_SUBSET_TYPE_WITH_NO_PROPERTIES, code)
    }

    @Test
    fun `kubernetes function with with pulumi(dot)json#(slash)Json argument can be invoked`() {
        // language=kotlin
        val code = """
            import com.pulumi.kubernetes.apiextensions.v1.kotlin.outputs.CustomResourceSubresources
            import kotlinx.serialization.json.JsonObject
            import kotlinx.serialization.json.JsonPrimitive


            suspend fun main() {
                CustomResourceSubresources(
                    status = JsonObject(
                        mapOf(
                            "field1" to JsonPrimitive("value1"),
                            "field2" to JsonPrimitive(2),
                            "field3" to JsonObject(
                                mapOf(
                                    "nestedField1" to JsonPrimitive("value3"),
                                    "nestedField2" to JsonPrimitive(4),
                                ),
                            ),
                        ),
                    )
                )
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_KUBERNETES_SUBSET_WITH_JSON, code)
    }

    @Test
    fun `code generated from schema with isOverlay resource can be compiled`() {
        assertGeneratedCodeCompiles(SCHEMA_KUBERNETES_SUBSET_WITH_IS_OVERLAY)
    }

    @Test
    fun `type with dollar sign in parameter name is generated`() {
        // language=kotlin
        val code = """
            import com.pulumi.kubernetes.apiextensions.v1.kotlin.customResourceDefinitionPatchResource


            suspend fun main() {
                customResourceDefinitionPatchResource("name") {
                    args {
                        spec {
                            versions(
                                {
                                    schema {
                                        openAPIV3Schema {
                                            ref("ref")
                                            schema("schema")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_KUBERNETES_SUBSET_WITH_DOLLAR_IN_PROPERTY_NAME, code)
    }

    @Test
    fun `type with java keyword in parameter name is generated`() {
        // language=kotlin
        val code = """
            import com.pulumi.kubernetes.core.v1.kotlin.limitRangeResource


            suspend fun main() {
                limitRangeResource("name") {
                    args {
                        spec {
                            limits(
                                {
                                    default("key" to "value")
                                },
                            )
                        }
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_KUBERNETES_SUBSET_WITH_JAVA_KEYWORD_IN_PROPERTY_NAME, code)
    }

    @Test
    fun `type with kotlin keyword in parameter name is generated`() {
        // language=kotlin
        val code = """
            import com.pulumi.kubernetes.autoscaling.v2.kotlin.horizontalPodAutoscalerResource


            suspend fun main() {
                horizontalPodAutoscalerResource("name") {
                    args {
                        spec {
                            metrics(
                                {
                                    `object` {
                                        metric {
                                            name("object")
                                        }
                                    }
                                    `external` {
                                        metric {
                                            name("exterbal")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_KUBERNETES_SUBSET_WITH_KOTLIN_KEYWORD_IN_PROPERTY_NAME, code)
    }

    @Test
    fun `resource with lowercase name is generated`() {
        // language=kotlin
        val code = """
            import com.pulumi.azurenative.aadiam.kotlin.azureADMetricResource
            import com.pulumi.azurenative.aadiam.kotlin.AzureADMetric


            suspend fun main() {
                val resource: AzureADMetric = azureADMetricResource("name") {
                    args {
                        location("location")
                    }
                }
            }
        """

        assertGeneratedCodeAndSourceFileCompile(SCHEMA_AZURE_NATIVE_SUBSET_WITH_LOWERCASE_RESOURCE, code)
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
        val compilationResult =
            generateCodeAndCompileAsSeparateModules(schemaPath, sourceFiles, ejectIfStatusIsNot = COMPILATION_ERROR)

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
        val compilationResult =
            generateCodeAndCompileAsSeparateModules(schemaPath, sourceFiles, ejectIfStatusIsNot = OK)

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
        ejectIfStatusIsNot: KotlinCompilation.ExitCode = COMPILATION_ERROR,
    ): AggregateCompilationResult {
        val outputDirectory = Codegen.codegen(loadResource("/$schemaPath"))
        val generatedSourceFiles = readFilesRecursively(outputDirectory)
            .map { (fileName, contents) -> SourceFile.kotlin(fileName, contents) }

        val additionalSourceFiles = sourceFiles
            .map { (fileName, source) -> SourceFile.new(fileName, source.trimIndent()) }

        val classPathWithDependencies = dependencies.map { downloadedDependency(it) }

        val compilationForGeneratedCode = KotlinCompilation().apply {
            sources = generatedSourceFiles
            classpaths = classPathWithDependencies
        }

        val compilationForAdditionalCode = KotlinCompilation().apply {
            sources = additionalSourceFiles
            classpaths = classPathWithDependencies + compilationForGeneratedCode.classesDir
        }

        val compiledGeneratedCode = compilationForGeneratedCode.compile()
        val compiledAdditionalCode = compilationForAdditionalCode.compile()

        val shouldEject = compiledGeneratedCode.exitCode != ejectIfStatusIsNot ||
            compiledAdditionalCode.exitCode != ejectIfStatusIsNot

        if (shouldEject) {
            val tempDirectory = createTempDirectory().toFile()
            sourceFiles.forEach { (fileName, source) ->
                val file = tempDirectory.resolve(fileName)
                file.writeText(source)
            }

            ejectToMavenProject(
                dependencies,
                listOf(outputDirectory, tempDirectory),
                testInfo.displayName,
            )
        }

        return AggregateCompilationResult.from(
            mapOf(
                "generatedCode" to compiledGeneratedCode,
                "additionalCode" to compiledAdditionalCode,
            ),
        )
    }

    private fun downloadedDependency(coordinate: String) =
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
private const val SCHEMA_AWS_CLASSIC_SUBSET_WITH_PROVIDER_AND_CERTIFICATE =
    "schema-aws-classic-5.16.2-subset-with-certificate-and-provider.json"
private const val SCHEMA_SLACK_SUBSET_WITH_INDEX = "schema-slack-0.3.0-subset-with-index.json"
private const val SCHEMA_AZURE_NATIVE_SUBSET_WITH_IP_ALLOCATION =
    "schema-azure-native-3.44.2-subset-with-ip-allocation.json"
private const val SCHEMA_GITHUB_SUBSET_WITH_NAME_COLLISION = "schema-github-4.17.0-subset-with-name-collision.json"
private const val SCHEMA_GOOGLE_NATIVE_SUBSET_WITH_INVALID_NAME =
    "schema-google-native-0.27.0-subset-with-invalid-name.json"
private const val SCHEMA_GOOGLE_NATIVE_SUBSET_WITH_KEYWORD_TYPE_PROPERTY =
    "schema-google-native-0.27.0-subset-with-keyword-type-property.json"
private const val SCHEMA_GOOGLE_NATIVE_SUBSET_NAMESPACE_WITH_SLASH =
    "schema-google-native-0.27.0-subset-namespace-with-slash.json"
private const val SCHEMA_GOOGLE_NATIVE_SUBSET_TYPE_WITH_NO_PROPERTIES =
    "schema-google-native-0.27.0-subset-type-with-no-properties.json"
private const val SCHEMA_KUBERNETES_SUBSET_WITH_JSON = "schema-kubernetes-3.22.1-subset-with-json.json"
private const val SCHEMA_KUBERNETES_SUBSET_WITH_IS_OVERLAY = "schema-kubernetes-3.22.1-subset-with-is-overlay.json"
private const val SCHEMA_KUBERNETES_SUBSET_WITH_DOLLAR_IN_PROPERTY_NAME =
    "schema-kubernetes-3.22.1-subset-with-dollar-in-property-name.json"
private const val SCHEMA_KUBERNETES_SUBSET_WITH_JAVA_KEYWORD_IN_PROPERTY_NAME =
    "schema-kubernetes-3.22.1-subset-with-java-keyword-in-property-name.json"
private const val SCHEMA_KUBERNETES_SUBSET_WITH_KOTLIN_KEYWORD_IN_PROPERTY_NAME =
    "schema-kubernetes-3.22.1-subset-with-kotlin-keyword-in-property-name.json"
private const val SCHEMA_AZURE_NATIVE_SUBSET_WITH_LOWERCASE_RESOURCE =
    "schema-azure-native-1.85.0-subset-with-lowercase-resource.json"

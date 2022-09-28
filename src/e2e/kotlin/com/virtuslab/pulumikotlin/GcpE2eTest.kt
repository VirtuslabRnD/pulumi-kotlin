package com.virtuslab.pulumikotlin

import com.google.cloud.compute.v1.AggregatedListInstancesRequest
import com.google.cloud.compute.v1.Instance
import com.google.cloud.compute.v1.InstancesClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertContains

private const val PROJECT_NAME = "jvm-lab"

class GcpE2eTest {

    var stackName: String = ""
    var fullStackName: String = ""

    @BeforeTest
    fun setupTest() {
        stackName = "test${RandomStringUtils.randomNumeric(10)}"
        fullStackName = "$PROJECT_NAME/gcp-sample-project/$stackName"
    }

    @Test
    fun `gcp VM instance can be created`() {
        runProcess("pulumi", "stack", "init", fullStackName)
        runProcess("pulumi", "up", "-y", "-s", fullStackName, "-c", "gcp:project=$PROJECT_NAME")

        val stackOutput = runProcess("pulumi", "stack", "output", "-s", fullStackName, "--json")
        val parsedStackOutput = Json.decodeFromString<StackOutput>(stackOutput)

        val instance = getInstance(parsedStackOutput.instanceName)

        assertContains(instance.machineType, "e2-micro")

        val tags: Iterable<String> = instance.tags?.itemsList ?: listOf()
        assertContains(tags, "foo")
        assertContains(tags, "bar")

        val attachedDisk = instance.disksList[0]
        assert(attachedDisk.boot)

        assertContains(instance.networkInterfacesList[0].network, "default")

        val metadata = instance.metadata.itemsList.map { (it.key to it.value) }
        assertContains(metadata, "foo" to "bar")
        assertContains(metadata, "startup-script" to "echo hi > /test.txt")
    }

    @AfterTest
    fun cleanupTest() {
        runProcess("pulumi", "destroy", "-y", "-s", fullStackName)
        runProcess("pulumi", "stack", "rm", "jvm-lab/$stackName", "-y")
    }

    private fun runProcess(vararg command: String): String {
        val process = ProcessBuilder(command.asList())
            .directory(File("${Paths.get("").toAbsolutePath()}/examples/gcp-sample-project"))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
        return process.inputStream.bufferedReader().readText()
    }

    private fun getInstance(instanceId: String): Instance {
        val instancesClient = InstancesClient.create()
        val aggregatedListInstancesRequest = AggregatedListInstancesRequest
            .newBuilder()
            .setProject(PROJECT_NAME)
            .setFilter("name eq $instanceId")
            .setMaxResults(1)
            .build()

        return instancesClient
            .aggregatedList(aggregatedListInstancesRequest)
            .iterateAll()
            .first { (_, value) -> value.instancesList.isNotEmpty() }
            ?.value
            ?.instancesList
            ?.firstOrNull()!!
    }

    @Serializable
    data class StackOutput(val instanceName: String)
}

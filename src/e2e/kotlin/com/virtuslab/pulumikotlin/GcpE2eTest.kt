package com.virtuslab.pulumikotlin

import com.google.cloud.compute.v1.AggregatedListInstancesRequest
import com.google.cloud.compute.v1.Instance
import com.google.cloud.compute.v1.InstancesClient
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertContains

private const val PROJECT_NAME = "jvm-lab"

class GcpE2eTest {

    @Test
    fun `gcp VM instance can be created`() {
        val stackName = "test${RandomStringUtils.randomNumeric(10)}"
        val fullStackName = "$PROJECT_NAME/gcp-sample-project/$stackName"

        runProcess("pulumi", "stack", "init", fullStackName)
            .assertOutputContainsString("Created stack 'jvm-lab/$stackName'")

        runProcess("pulumi", "up", "-y", "-s", fullStackName, "-c", "gcp:project=$PROJECT_NAME")
            .assertOutputContainsString("+ 2 created")

        val instanceId = runProcess("pulumi", "stack", "-i", "-s", fullStackName)
            .findInstanceIdInOutput()

        val instance = getInstance(instanceId)

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

        runProcess("pulumi", "destroy", "-y", "-s", fullStackName)
            .assertOutputContainsString("- 2 deleted")

        runProcess("pulumi", "stack", "rm", "jvm-lab/$stackName", "-y")
            .assertOutputContainsString("Stack 'jvm-lab/$stackName' has been removed!")
    }

    private fun Process.findInstanceIdInOutput(): String {
        return Regex(".*projects/jvm-lab/zones/europe-central2-a/instances/(.*)")
            .find(
                inputStream.bufferedReader().readText(),
            )
            ?.groups
            ?.get(1)
            ?.value!!
    }

    private fun runProcess(vararg command: String): Process {
        val process = ProcessBuilder(command.asList())
            .directory(File("${Paths.get("").toAbsolutePath()}/examples/gcp-sample-project"))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
        return process!!
    }

    private fun Process.assertOutputContainsString(assertion: String) {
        assertContains(
            inputStream.bufferedReader().readText(),
            assertion,
        )
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
}

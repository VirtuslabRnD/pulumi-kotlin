@file:Suppress("FunctionName")

package com.virtuslab.pulumikotlin.gcp

import com.virtuslab.pulumikotlin.PROJECT_NAME
import com.virtuslab.pulumikotlin.Pulumi
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class GcpE2eTest {

    private lateinit var pulumi: Pulumi

    @Test
    fun `GCP VM instance can be created`() {
        // given
        val exampleName = "gcp-sample-project"
        val rootDirectory = getExampleDirectory(exampleName)
        val fullStackName = getExampleStackName(exampleName)

        // when
        pulumi = Pulumi(fullStackName, rootDirectory)
        pulumi.initStack()
        pulumi.up("gcp:project=$PROJECT_NAME")

        // then
        val instance = getInstance(pulumi.getStackOutput<PulumiStackOutput>().instanceName)

        assertVmExists(instance)
    }

    @Test
    fun `provider resource is correctly used`() {
        // given
        val exampleName = "gcp-provider-sample-project"
        val rootDirectory = getExampleDirectory(exampleName)
        val fullStackName = getExampleStackName(exampleName)

        // when
        pulumi = Pulumi(fullStackName, rootDirectory)
        pulumi.initStack()
        pulumi.up("gcp:project=$PROJECT_NAME")

        // then
        val outputProperties = pulumi.getStackOutput<Map<String, String>>()

        val instanceEuropeCentral2AName =
            requireNotNull(outputProperties["gcp-provider-sample-project-instance-europe-central2-a-name"])
        val instanceEuropeNorth1CName =
            requireNotNull(outputProperties["gcp-provider-sample-project-instance-europe-north1-c-name"])

        val instanceEuropeCentral2A = getInstance(instanceEuropeCentral2AName)
        val instanceEuropeNorth1C = getInstance(instanceEuropeNorth1CName)

        assertEquals(
            "europe-central2-a",
            outputProperties["gcp-provider-sample-project-instance-europe-central2-a-zone"],
        )
        assertEquals(
            "europe-north1-c",
            outputProperties["gcp-provider-sample-project-instance-europe-north1-c-zone"],
        )

        assertVmExists(instanceEuropeCentral2A)
        assertVmExists(instanceEuropeNorth1C)
    }

    private fun getExampleStackName(exampleName: String) =
        "$PROJECT_NAME/$exampleName/test${RandomStringUtils.randomNumeric(10)}"

    private fun getExampleDirectory(exampleName: String) = File("examples/$exampleName")

    @AfterEach
    fun cleanupTest() {
        pulumi.destroy()
        pulumi.rmStack()
    }
}

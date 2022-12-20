@file:Suppress("FunctionName")

package com.virtuslab.pulumikotlin.azure

import com.virtuslab.pulumikotlin.PROJECT_NAME
import com.virtuslab.pulumikotlin.Pulumi
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.AfterTest
class AzureNativeE2eTest {

    private lateinit var pulumi: Pulumi

    @Test
    fun `Azure native virtual machine can be created`() {
        val exampleName = "azure-native-sample-project"
        val rootDirectory = File("examples/$exampleName")
        val fullStackName = "$PROJECT_NAME/$exampleName/test${RandomStringUtils.randomNumeric(10)}"

        pulumi = Pulumi(fullStackName, rootDirectory)
        pulumi.initStack()
        pulumi.up(
            configOptions = mapOf("azure-native:location" to "westeurope"),
            environment = mapOf(
                "ARM_TENANT_ID" to System.getenv("ARM_TENANT_ID"),
                "ARM_SUBSCRIPTION_ID" to System.getenv("ARM_SUBSCRIPTION_ID"),
                "ARM_CLIENT_ID" to System.getenv("ARM_CLIENT_ID"),
                "ARM_CLIENT_SECRET" to System.getenv("ARM_CLIENT_SECRET"),
            ),
        )

        val virtualMachine = getVirtualMachine(pulumi.getStackOutput<PulumiStackOutput>().virtualMachineId)

        assertVmExists(virtualMachine)
    }

    @AfterTest
    fun cleanupTest() {
        pulumi.destroy()
        pulumi.rmStack()
    }
}

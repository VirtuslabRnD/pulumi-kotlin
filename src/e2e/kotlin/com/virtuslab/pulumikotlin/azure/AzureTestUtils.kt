package com.virtuslab.pulumikotlin.azure

import com.azure.core.management.AzureEnvironment.AZURE
import com.azure.core.management.profile.AzureProfile
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.resourcemanager.compute.ComputeManager
import com.azure.resourcemanager.compute.models.VirtualMachine
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes
import kotlinx.serialization.Serializable
import kotlin.test.assertContains
import kotlin.test.assertEquals

fun getVirtualMachine(virtualMachineId: String): VirtualMachine {
    val clientId = System.getenv("ARM_CLIENT_ID")
    val clientSecret = System.getenv("ARM_CLIENT_SECRET")
    val subscriptionId = System.getenv("ARM_SUBSCRIPTION_ID")
    val tenantId = System.getenv("ARM_TENANT_ID")

    val computeManager = ComputeManager.authenticate(
        ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .build(),
        AzureProfile(tenantId, subscriptionId, AZURE),
    )

    return computeManager.virtualMachines().getById(virtualMachineId)
}

fun assertVmExists(virtualMachine: VirtualMachine) {
    assertEquals(VirtualMachineSizeTypes.STANDARD_B1S, virtualMachine.size())

    val tags: Map<String, String> = virtualMachine.tags()
    assertContains(tags, "foo")
    assertEquals("bar", tags["foo"])
}

@Serializable
data class PulumiStackOutput(val virtualMachineId: String)

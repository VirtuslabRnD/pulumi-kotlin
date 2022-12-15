package com.virtuslab.pulumikotlin.azure

import com.azure.core.management.AzureEnvironment.AZURE
import com.azure.core.management.profile.AzureProfile
import com.azure.identity.DefaultAzureCredentialBuilder
import com.azure.resourcemanager.compute.ComputeManager
import com.azure.resourcemanager.compute.models.VirtualMachine
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes
import kotlinx.serialization.Serializable
import kotlin.test.assertContains
import kotlin.test.assertEquals

fun getVirtualMachine(virtualMachineId: String): VirtualMachine {
    // The Azure tenant/subscription that is used here belongs to @myhau (Michal Fudala)
    val subscriptionId = "3565bcd1-dd9b-45a8-9d7c-8880aaaa9a9f"
    val tenantId = "1ff3e4bd-41d1-4df2-b776-8d3fcbf77184"
    val computeManager = ComputeManager.authenticate(
        DefaultAzureCredentialBuilder().build(),
        AzureProfile(tenantId, subscriptionId, AZURE),
    )

    return computeManager.virtualMachines().getById(virtualMachineId)
}

fun assertVmExists(virtualMachine: VirtualMachine) {
    assertEquals(VirtualMachineSizeTypes.BASIC_A0, virtualMachine.size())

    val tags: Map<String, String> = virtualMachine.tags()
    assertContains(tags, "foo")
    assertEquals("bar", tags["foo"])
}

@Serializable
data class PulumiStackOutput(val virtualMachineId: String)

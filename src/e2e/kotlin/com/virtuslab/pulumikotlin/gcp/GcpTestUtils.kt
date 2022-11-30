package com.virtuslab.pulumikotlin.gcp

import com.google.cloud.compute.v1.AggregatedListInstancesRequest
import com.google.cloud.compute.v1.Instance
import com.google.cloud.compute.v1.InstancesClient
import com.virtuslab.pulumikotlin.PROJECT_NAME
import kotlinx.serialization.Serializable
import kotlin.test.assertContains
import kotlin.test.assertTrue

fun getInstance(instanceId: String): Instance {
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
        .flatMap { (_, value) -> value.instancesList }
        .first()
}

fun assertVmExists(instance: Instance) {
    assertContains(instance.machineType, "e2-micro")

    val tags: Iterable<String> = instance.tags?.itemsList.orEmpty()
    assertContains(tags, "foo")
    assertContains(tags, "bar")

    val attachedDisk = instance.disksList.first()
    assertTrue(attachedDisk.boot)

    assertContains(instance.networkInterfacesList.first().network, "default")

    val metadata = instance.metadata.itemsList.map { it.key to it.value }
    assertContains(metadata, "foo" to "bar")
    assertContains(metadata, "startup-script" to "echo hi > /test.txt")
}

@Serializable
data class InstanceTestInstanceStackOutput(val instanceName: String, val instanceZone: String? = null)

@Serializable
data class ProviderTestInstanceStackOutput(
    val instanceEuropeCentral2A: InstanceTestInstanceStackOutput,
    val instanceEuropeNorth1C: InstanceTestInstanceStackOutput,
)

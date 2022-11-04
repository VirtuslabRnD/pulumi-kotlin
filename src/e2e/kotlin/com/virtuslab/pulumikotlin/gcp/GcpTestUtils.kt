package com.virtuslab.pulumikotlin.gcp

import com.google.cloud.compute.v1.AggregatedListInstancesRequest
import com.google.cloud.compute.v1.Instance
import com.google.cloud.compute.v1.InstancesClient

const val PROJECT_NAME = "jvm-lab"

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

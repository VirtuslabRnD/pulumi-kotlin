@file:Suppress("FunctionName")

package org.virtuslab.pulumikotlin.kubernetes

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.util.Config
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import org.virtuslab.pulumikotlin.PROJECT_NAME
import org.virtuslab.pulumikotlin.Pulumi
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.assertContains
import kotlin.test.assertEquals

class KubernetesE2eTest {

    private lateinit var pulumi: Pulumi

    @Test
    fun `Kubernetes deployment can be created`() {
        val exampleName = "kubernetes-sample-project"
        val rootDirectory = File("examples/$exampleName")
        val fullStackName = "$PROJECT_NAME/$exampleName/test${RandomStringUtils.randomNumeric(10)}"

        pulumi = Pulumi(fullStackName, rootDirectory)
        pulumi.initStack()
        pulumi.up("kubernetes:context" to "minikube")

        val pod = getCreatedPod(pulumi.getStackOutput<PulumiStackOutput>().name)

        assertContains(
            pod?.metadata?.labels?.entries.orEmpty().map { it.toPair() },
            "app" to "nginx",
        )

        assertEquals(1, pod?.spec?.containers?.size)
        val container = pod?.spec?.containers?.get(0)
        assertEquals(container?.name, "nginx")
        assertEquals(container?.image, "nginx")

        assertEquals(1, container?.ports?.size)
        assertEquals(container?.ports?.get(0)?.containerPort, 80)
    }

    @AfterTest
    fun cleanupTest() {
        pulumi.destroy()
        pulumi.rmStack()
    }

    private fun getCreatedPod(podName: String): V1Pod? {
        val client: ApiClient = Config.defaultClient()
        Configuration.setDefaultApiClient(client)
        val api = CoreV1Api()

        return api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null)
            .items
            .first { it.metadata?.name?.startsWith(podName) ?: false }
    }

    @Serializable
    private data class PulumiStackOutput(val name: String)
}

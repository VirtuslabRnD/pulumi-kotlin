package com.virtuslab.pulumikotlin.gcp

import com.virtuslab.pulumikotlin.Pulumi
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertContains

class GoogleNativeE2eTest {

    private val rootDirectory = File("examples/google-native-sample-project")
    private var pulumi: Pulumi? = null

    @BeforeTest
    fun setupTest() {
        val fullStackName = "$PROJECT_NAME/google-native-sample-project/test${RandomStringUtils.randomNumeric(10)}"
        pulumi = Pulumi(fullStackName, rootDirectory)
        pulumi!!.initStack()
        pulumi!!.up("google-native:project=$PROJECT_NAME")
    }

    @Test
    fun `GCP VM instance can be created`() {
        val parsedStackOutput = pulumi!!.stackOutput()

        val instance = getInstance(parsedStackOutput.instanceName)

        assertContains(instance.machineType, "e2-micro")

        val tags: Iterable<String> = instance.tags?.itemsList.orEmpty()
        assertContains(tags, "foo")
        assertContains(tags, "bar")

        val attachedDisk = instance.disksList.first()
        assertTrue("", attachedDisk.boot)

        assertContains(instance.networkInterfacesList.first().network, "default")

        val metadata = instance.metadata.itemsList.map { it.key to it.value }
        assertContains(metadata, "foo" to "bar")
        assertContains(metadata, "startup-script" to "echo hi > /test.txt")
    }

    @AfterTest
    fun cleanupTest() {
        pulumi!!.destroy()
        pulumi!!.rmStack()
    }
}

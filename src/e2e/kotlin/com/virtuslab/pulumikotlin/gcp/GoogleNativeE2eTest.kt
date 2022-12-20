@file:Suppress("FunctionName")

package com.virtuslab.pulumikotlin.gcp

import com.virtuslab.pulumikotlin.PROJECT_NAME
import com.virtuslab.pulumikotlin.Pulumi
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.AfterTest

class GoogleNativeE2eTest {

    private lateinit var pulumi: Pulumi

    @Test
    fun `GCP VM instance can be created`() {
        val exampleName = "google-native-sample-project"
        val rootDirectory = File("examples/$exampleName")
        val fullStackName = "$PROJECT_NAME/$exampleName/test${RandomStringUtils.randomNumeric(10)}"

        pulumi = Pulumi(fullStackName, rootDirectory)
        pulumi.initStack()
        pulumi.up("google-native:project" to PROJECT_NAME)

        val instance = getInstance(pulumi.getStackOutput<InstanceTestInstanceStackOutput>().instanceName)

        assertVmExists(instance)
    }

    @AfterTest
    fun cleanupTest() {
        pulumi.destroy()
        pulumi.rmStack()
    }
}

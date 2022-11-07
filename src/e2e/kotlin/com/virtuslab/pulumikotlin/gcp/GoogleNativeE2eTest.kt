@file:Suppress("FunctionName")

package com.virtuslab.pulumikotlin.gcp

import com.virtuslab.pulumikotlin.Pulumi
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

private const val EXAMPLE_NAME = "google-native-sample-project"

class GoogleNativeE2eTest {

    private val rootDirectory = File("examples/$EXAMPLE_NAME")
    private var pulumi: Pulumi? = null

    @BeforeTest
    fun setupTest() {
        val fullStackName = "$PROJECT_NAME/$EXAMPLE_NAME/test${RandomStringUtils.randomNumeric(10)}"
        pulumi = Pulumi(fullStackName, rootDirectory)
        pulumi!!.initStack()
        pulumi!!.up("google-native:project=$PROJECT_NAME")
    }

    @Test
    fun `GCP VM instance can be created`() {
        createVmAndVerifyItsExistence(pulumi!!)
    }

    @AfterTest
    fun cleanupTest() {
        pulumi!!.destroy()
        pulumi!!.rmStack()
    }
}

package com.virtuslab.pulumikotlin

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PulumiStackOutput(val instanceName: String)

class Pulumi(private val fullStackName: String, private val rootDirectory: File) {

    fun initStack() {
        runProcess(rootDirectory, "pulumi", "stack", "init", fullStackName)
    }

    fun up(vararg configOptions: String) {
        val config = if (configOptions.isNotEmpty()) listOf("-c") + configOptions else emptyList()
        runProcess(rootDirectory, "pulumi", "up", "-y", "-s", fullStackName, *config.toTypedArray())
    }

    fun getStackOutput(): PulumiStackOutput {
        val stackOutput = runProcess(rootDirectory, "pulumi", "stack", "output", "-s", fullStackName, "--json")
        return Json.decodeFromString(stackOutput)
    }

    fun destroy() {
        runProcess(rootDirectory, "pulumi", "destroy", "-y", "-s", fullStackName)
    }

    fun rmStack() {
        runProcess(rootDirectory, "pulumi", "stack", "rm", fullStackName, "-y")
    }
}

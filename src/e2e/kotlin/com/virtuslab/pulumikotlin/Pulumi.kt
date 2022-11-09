package com.virtuslab.pulumikotlin

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class Pulumi(val fullStackName: String, val rootDirectory: File) {

    fun initStack() {
        runProcess(rootDirectory, "pulumi", "stack", "init", fullStackName)
    }

    fun up(vararg configOptions: String) {
        val config = configOptions.map { "-c $it" }
        runProcess(rootDirectory, "pulumi", "up", "-y", "-s", fullStackName, *config.toTypedArray())
    }

    inline fun <reified T> getStackOutput(): T = Json.decodeFromString(
        runProcess(
            rootDirectory,
            "pulumi",
            "stack",
            "output",
            "-s",
            fullStackName,
            "--json",
        ),
    )

    fun destroy() {
        runProcess(rootDirectory, "pulumi", "destroy", "-y", "-s", fullStackName)
    }

    fun rmStack() {
        runProcess(rootDirectory, "pulumi", "stack", "rm", fullStackName, "-y")
    }
}

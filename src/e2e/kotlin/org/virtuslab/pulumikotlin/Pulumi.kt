package org.virtuslab.pulumikotlin

import kotlinx.serialization.json.Json
import java.io.File

class Pulumi(val fullStackName: String, val rootDirectory: File) {

    fun initStack() {
        runProcess(rootDirectory, "pulumi", "stack", "init", fullStackName)
    }

    fun up(configOptions: Map<String, String>, environment: Map<String, String> = emptyMap()) {
        val config = configOptions.flatMap { (key, value) -> listOf("-c", "$key=$value") }
        runProcess(
            rootDirectory,
            listOf("pulumi", "up", "-y", "-s", fullStackName) + config,
            environment = environment,
        )
    }

    fun up(vararg configOptions: Pair<String, String>) {
        up(configOptions.toMap())
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

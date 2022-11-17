package com.virtuslab.pulumikotlin

import java.io.File
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.ProcessBuilder.Redirect.PIPE
import kotlin.test.fail

const val PROJECT_NAME = "jvm-lab"

fun runProcess(rootDirectory: File, vararg command: String): String {
    val process = ProcessBuilder(command.asList())
        .directory(rootDirectory)
        .redirectOutput(PIPE)
        .redirectError(INHERIT)
        .start()
    process.waitFor()

    val exitValue = process.exitValue()
    val output = process.inputStream.bufferedReader().readText()
    if (exitValue != 0) {
        val readableCommand = command.joinToString(" ")
        fail(
            """
                    Exit code of command "$readableCommand" was $exitValue.
                    Process output:
                    $output
            """.trimIndent(),
        )
    }

    return output
}

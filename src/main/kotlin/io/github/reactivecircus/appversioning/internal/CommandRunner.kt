package io.github.reactivecircus.appversioning.internal

import java.io.File
import java.util.concurrent.TimeUnit

internal fun String.execute(workingDir: File, timeoutInSeconds: Long = DEFAULT_COMMAND_TIMEOUT_SECONDS): String {
    val parts = this.split("\\s".toRegex())
    val process = ProcessBuilder(parts)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    process.waitFor(timeoutInSeconds, TimeUnit.SECONDS)
    return process.inputStream.bufferedReader().readText().trim()
}

private const val DEFAULT_COMMAND_TIMEOUT_SECONDS = 5L

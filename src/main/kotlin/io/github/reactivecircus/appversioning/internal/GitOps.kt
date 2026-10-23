package io.github.reactivecircus.appversioning.internal

import java.io.File
import java.util.concurrent.TimeUnit

internal object GitOps {
    fun ensureValidGitRepo(workingDir: File) {
        val isValid = runCatching {
            listOf("git", "rev-parse", "--is-inside-work-tree").execute(workingDir).toBoolean()
        }.getOrDefault(false)
        require(isValid)
    }

    fun listLocalTags(workingDir: File): List<String> {
        val output = listOf("git", "tag", "--list").execute(workingDir)
        return if (output.isEmpty()) emptyList() else output.lines()
    }

    fun fetchRemoteTags(workingDir: File) {
        listOf("git", "fetch", "--tags").execute(workingDir)
    }

    fun describeLatestTag(workingDir: File, pattern: String? = null): String? {
        val command = buildList {
            add("git")
            add("describe")
            if (pattern != null) {
                add("--match")
                add(pattern)
            }
            add("--tags")
            add("--long")
        }
        return runCatching { command.execute(workingDir) }.getOrNull()
    }
}

private fun List<String>.execute(workingDir: File, timeoutInSeconds: Long = DEFAULT_COMMAND_TIMEOUT_SECONDS): String {
    val process = ProcessBuilder(this)
        .directory(workingDir)
        .start()
    if (!process.waitFor(timeoutInSeconds, TimeUnit.SECONDS)) {
        process.destroy()
        throw IllegalStateException("Execution timeout: $this")
    }
    if (process.exitValue() != 0) {
        throw IllegalStateException("Execution failed with exit code: ${process.exitValue()}: $this")
    }
    return process.inputStream.bufferedReader().readText().trim()
}

private const val DEFAULT_COMMAND_TIMEOUT_SECONDS = 5L

package io.github.reactivecircus.appversioning.internal

import java.io.File
import java.util.concurrent.TimeUnit

internal class GitClient private constructor(private val workingDir: File) {

    fun listLocalTags(): List<String> {
        return "git tag --list".execute(workingDir).lines()
    }

    fun fetchRemoteTags() {
        "git fetch --tag".execute(workingDir)
    }

    fun describeLatestTag(pattern: String? = null): String? {
        val matchPattern = pattern?.let { "--match $it" } ?: ""
        return runCatching {
            "git describe $matchPattern --tags --long".execute(workingDir)
        }.getOrNull()
    }

    companion object {

        fun initialize(workingDir: File): GitClient {
            return GitClient(workingDir).apply {
                "git init".execute(workingDir)
            }
        }

        fun open(workingDir: File): GitClient {
            require(workingDir.isInValidGitRepo)
            return GitClient(workingDir)
        }
    }

    // TODO add String -> GitTag parser
}

internal val File.isInValidGitRepo: Boolean
    get() = runCatching {
        "git rev-parse --is-inside-work-tree".execute(this).toBoolean()
    }.getOrDefault(false)

private fun String.execute(workingDir: File, timeoutInSeconds: Long = DEFAULT_COMMAND_TIMEOUT_SECONDS): String {
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

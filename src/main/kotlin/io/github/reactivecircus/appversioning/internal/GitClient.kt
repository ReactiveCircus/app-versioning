package io.github.reactivecircus.appversioning.internal

import java.io.File
import java.util.concurrent.TimeUnit

class GitClient private constructor(private val projectDir: File) {

    fun listLocalTags(): List<String> {
        return listOf("git", "tag", "--list").execute(projectDir).lines()
    }

    fun fetchRemoteTags() {
        listOf("git", "fetch", "--tags").execute(projectDir)
    }

    fun describeLatestTag(pattern: String? = null): String? {
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
        return runCatching { command.execute(projectDir) }.getOrNull()
    }

    fun commit(message: String, allowEmpty: Boolean = true): CommitId {
        val commitCommand = buildList {
            add("git")
            add("commit")
            if (allowEmpty) {
                add("--allow-empty")
            }
            add("-m")
            add(message)
        }
        commitCommand.execute(projectDir)

        val getCommitIdCommand = listOf("git", "rev-parse", "--short", "HEAD")
        return CommitId(getCommitIdCommand.execute(projectDir))
    }

    fun tag(name: String, message: String, commitId: CommitId? = null) {
        val command = buildList {
            add("git")
            add("tag")
            add("-a")
            add(name)
            if (commitId != null) {
                add(commitId.value)
            }
            add("-m")
            add(message)
        }
        command.execute(projectDir)
    }

    fun checkoutTag(tag: String) {
        val commitCommand = buildList {
            add("git")
            add("checkout")
            add(tag)
        }
        commitCommand.execute(projectDir)
    }

    companion object {

        fun initialize(projectDir: File): GitClient {
            return GitClient(projectDir).apply {
                listOf("git", "init").execute(projectDir)
            }
        }

        fun open(projectDir: File): GitClient {
            require(projectDir.isInValidGitRepo)
            return GitClient(projectDir)
        }
    }
}

@JvmInline
value class CommitId(val value: String)

private val File.isInValidGitRepo: Boolean
    get() = runCatching {
        listOf("git", "rev-parse", "--is-inside-work-tree").execute(this).toBoolean()
    }.getOrDefault(false)

@Suppress("UseCheckOrError")
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

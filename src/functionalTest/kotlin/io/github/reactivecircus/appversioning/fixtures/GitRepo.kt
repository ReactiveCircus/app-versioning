package io.github.reactivecircus.appversioning.fixtures

import java.io.File
import java.util.concurrent.TimeUnit

fun initGitRepo(rootDir: File, action: GitRepo.() -> Unit = {}): GitRepo {
    return GitRepo(rootDir).apply(action)
}

class GitRepo(private val rootDir: File) {
    init {
        listOf("git", "init").execute(rootDir)
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
        commitCommand.execute(rootDir)

        val getCommitIdCommand = listOf("git", "rev-parse", "--short", "HEAD")
        return CommitId(getCommitIdCommand.execute(rootDir))
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
        command.execute(rootDir)
    }

    fun checkoutTag(tag: String) {
        val commitCommand = buildList {
            add("git")
            add("checkout")
            add(tag)
        }
        commitCommand.execute(rootDir)
    }
}

@JvmInline
value class CommitId(val value: String)

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

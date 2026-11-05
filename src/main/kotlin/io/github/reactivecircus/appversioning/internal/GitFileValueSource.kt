package io.github.reactivecircus.appversioning.internal

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

internal abstract class GitFileValueSource : ValueSource<File, GitFileValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val rootDirectory: DirectoryProperty
        val gitRootDirectory: DirectoryProperty
        val bareGitRepoDirectory: DirectoryProperty
        val gitFile: Property<GitFile>
    }

    enum class GitFile {
        Head,
        RefsDirectory,
    }

    override fun obtain(): File? {
        val bareGitRepoDirectory = parameters.bareGitRepoDirectory.asFile.orNull
        val gitRootDirectory = parameters.gitRootDirectory.asFile.orNull
        val gitDirectories = when {
            bareGitRepoDirectory != null -> GitDirectories.forGitDirectory(bareGitRepoDirectory)
            gitRootDirectory != null -> GitDirectories.forWorkTree(gitRootDirectory)
            else -> GitDirectories.forWorkTree(parameters.rootDirectory.get().asFile)
        } ?: return null
        val file = when (parameters.gitFile.get()) {
            GitFile.Head -> gitDirectories.headFile
            GitFile.RefsDirectory -> gitDirectories.refsDirectory
        }
        return file.takeIf { it.exists() }
    }
}

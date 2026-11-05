package io.github.reactivecircus.appversioning.internal

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

/**
 * Lazily locates the relevant git files or returns an empty list if no valid git directory is found.
 */
internal abstract class GitRefFilesValueSource : ValueSource<List<File>, GitRefFilesValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val rootDirectory: DirectoryProperty
        val gitRootDirectory: DirectoryProperty
        val bareGitRepoDirectory: DirectoryProperty
    }

    override fun obtain(): List<File> {
        val bareGitRepoDirectory = parameters.bareGitRepoDirectory.asFile.orNull
        val gitRootDirectory = parameters.gitRootDirectory.asFile.orNull
        val gitDirectories = when {
            bareGitRepoDirectory != null -> GitDirectories.forGitDirectory(bareGitRepoDirectory)
            gitRootDirectory != null -> GitDirectories.forWorkTree(gitRootDirectory)
            else -> GitDirectories.forWorkTree(parameters.rootDirectory.get().asFile)
        }
        val isValidGitDirectory = gitDirectories != null &&
            gitDirectories.headFile.exists() &&
            gitDirectories.refsDirectory.exists()
        return if (isValidGitDirectory) {
            gitDirectories.refFiles.filter { it.exists() }
        } else {
            emptyList()
        }
    }
}

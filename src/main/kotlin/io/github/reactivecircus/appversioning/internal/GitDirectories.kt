package io.github.reactivecircus.appversioning.internal

import java.io.File

/**
 * Locations of the git files that determine the result of `git describe`.
 *
 * For a linked worktree (created by `git worktree add`) or a submodule, `.git` is a file pointing to the actual git directory.
 * `HEAD` is per-worktree and lives in that git directory,
 * while the refs (branches and tags) are shared by all worktrees and live in the common git directory,
 * either as loose files in the refs directory or in the `packed-refs` file.
 *
 * With the reftable ref storage format, `HEAD` and the refs directory are stubs that never change.
 * Instead, the shared refs live in the `reftable` directory of the common git directory,
 * and the per-worktree refs (including `HEAD`) of a linked worktree live in the `reftable` directory of its git directory.
 */
internal class GitDirectories private constructor(gitDirectory: File, commonDirectory: File) {
    val headFile: File = gitDirectory.resolve(HEAD_FILE)

    val refsDirectory: File = commonDirectory.resolve(REFS_DIRECTORY)

    val packedRefsFile: File = commonDirectory.resolve(PACKED_REFS_FILE)

    val reftableDirectory: File = commonDirectory.resolve(REFTABLE_DIRECTORY)

    /**
     * The `reftable` directory of a linked worktree, or `null` if the git directory is the common git directory.
     */
    val worktreeReftableDirectory: File? = gitDirectory.resolve(REFTABLE_DIRECTORY)
        .takeIf { gitDirectory != commonDirectory }

    companion object {
        /**
         * Resolves the git directories of the working tree at [workTreeDirectory],
         * or returns `null` if [workTreeDirectory] has no valid `.git` directory or `.git` file.
         */
        fun forWorkTree(workTreeDirectory: File): GitDirectories? {
            val dotGit = workTreeDirectory.resolve(DOT_GIT)
            return when {
                dotGit.isDirectory -> forGitDirectory(dotGit)
                dotGit.isFile -> dotGit.readGitFile()?.let(::forGitDirectory)
                else -> null
            }
        }

        /**
         * Resolves the git directories of the git directory at [gitDirectory] (e.g. `.git` or a bare repository).
         */
        fun forGitDirectory(gitDirectory: File): GitDirectories {
            val commonDirFile = gitDirectory.resolve(COMMON_DIR_FILE)
            val commonDirectory = if (commonDirFile.isFile) {
                gitDirectory.resolve(commonDirFile.readText().trim()).normalize()
            } else {
                gitDirectory
            }
            return GitDirectories(gitDirectory, commonDirectory)
        }

        /**
         * Reads the git directory from a `.git` file containing `gitdir: <path>`,
         * where `<path>` is either absolute or relative to the directory containing the `.git` file.
         */
        private fun File.readGitFile(): File? {
            val content = readText().trim()
            if (!content.startsWith(GIT_DIR_PREFIX)) return null
            return content.removePrefix(GIT_DIR_PREFIX).trim()
                .takeIf { it.isNotEmpty() }
                ?.let { absoluteFile.parentFile.resolve(it).normalize() }
        }
    }
}

private const val DOT_GIT = ".git"
private const val HEAD_FILE = "HEAD"
private const val REFS_DIRECTORY = "refs"
private const val PACKED_REFS_FILE = "packed-refs"
private const val REFTABLE_DIRECTORY = "reftable"
private const val COMMON_DIR_FILE = "commondir"
private const val GIT_DIR_PREFIX = "gitdir:"

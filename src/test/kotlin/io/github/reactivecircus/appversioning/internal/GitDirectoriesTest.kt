package io.github.reactivecircus.appversioning.internal

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitDirectoriesTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `resolves HEAD and refs from the git directory when work tree has a git directory`() {
        val workTree = tempDir.newFolder("repo")
        workTree.resolve(".git").mkdirs()

        val gitDirectories = assertNotNull(GitDirectories.forWorkTree(workTree))

        assertEquals(workTree.resolve(".git/HEAD"), gitDirectories.headFile)
        assertEquals(workTree.resolve(".git/refs"), gitDirectories.refsDirectory)
        assertEquals(workTree.resolve(".git/packed-refs"), gitDirectories.packedRefsFile)
    }

    @Test
    fun `resolves HEAD from the worktree git directory and refs from the common git directory when work tree is a linked worktree`() {
        val mainWorkTree = tempDir.newFolder("main")
        val worktreeGitDir = mainWorkTree.resolve(".git/worktrees/feature").apply { mkdirs() }
        worktreeGitDir.resolve("commondir").writeText("../..\n")
        val linkedWorkTree = tempDir.newFolder("feature")
        linkedWorkTree.resolve(".git").writeText("gitdir: ${worktreeGitDir.absolutePath}\n")

        val gitDirectories = assertNotNull(GitDirectories.forWorkTree(linkedWorkTree))

        assertEquals(worktreeGitDir.resolve("HEAD"), gitDirectories.headFile)
        assertEquals(mainWorkTree.resolve(".git/refs"), gitDirectories.refsDirectory)
        assertEquals(mainWorkTree.resolve(".git/packed-refs"), gitDirectories.packedRefsFile)
    }

    @Test
    fun `resolves HEAD and refs from the gitdir relative to the work tree when work tree is a submodule`() {
        val superproject = tempDir.newFolder("superproject")
        val submoduleGitDir = superproject.resolve(".git/modules/lib").apply { mkdirs() }
        val submoduleWorkTree = superproject.resolve("lib").apply { mkdirs() }
        submoduleWorkTree.resolve(".git").writeText("gitdir: ../.git/modules/lib\n")

        val gitDirectories = assertNotNull(GitDirectories.forWorkTree(submoduleWorkTree))

        assertEquals(submoduleGitDir.resolve("HEAD"), gitDirectories.headFile)
        assertEquals(submoduleGitDir.resolve("refs"), gitDirectories.refsDirectory)
        assertEquals(submoduleGitDir.resolve("packed-refs"), gitDirectories.packedRefsFile)
    }

    @Test
    fun `resolves refs from an absolute common git directory`() {
        val commonGitDir = tempDir.newFolder("common.git")
        val worktreeGitDir = tempDir.newFolder("worktree-git-dir")
        worktreeGitDir.resolve("commondir").writeText(commonGitDir.absolutePath)

        val gitDirectories = GitDirectories.forGitDirectory(worktreeGitDir)

        assertEquals(worktreeGitDir.resolve("HEAD"), gitDirectories.headFile)
        assertEquals(commonGitDir.resolve("refs"), gitDirectories.refsDirectory)
        assertEquals(commonGitDir.resolve("packed-refs"), gitDirectories.packedRefsFile)
    }

    @Test
    fun `resolves HEAD and refs from a bare git repository`() {
        val bareRepo = tempDir.newFolder("app.git")

        val gitDirectories = GitDirectories.forGitDirectory(bareRepo)

        assertEquals(bareRepo.resolve("HEAD"), gitDirectories.headFile)
        assertEquals(bareRepo.resolve("refs"), gitDirectories.refsDirectory)
        assertEquals(bareRepo.resolve("packed-refs"), gitDirectories.packedRefsFile)
    }

    @Test
    fun `returns null when work tree has no git directory or git file`() {
        val workTree = tempDir.newFolder("not-a-repo")

        assertNull(GitDirectories.forWorkTree(workTree))
    }

    @Test
    fun `returns null when git file does not contain a gitdir`() {
        listOf("", "gitdir:", "gitdir:   \n", "not a git file").forEach { content ->
            val workTree = tempDir.newFolder()
            workTree.resolve(".git").writeText(content)

            assertNull(GitDirectories.forWorkTree(workTree), "Expected null for git file content: \"$content\"")
        }
    }
}

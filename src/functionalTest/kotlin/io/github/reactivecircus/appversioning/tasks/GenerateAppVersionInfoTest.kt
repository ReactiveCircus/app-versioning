@file:Suppress("FunctionName", "DuplicatedCode")

package io.github.reactivecircus.appversioning.tasks

import io.github.reactivecircus.appversioning.fixtures.AppProjectTemplate
import io.github.reactivecircus.appversioning.fixtures.BuildScriptLanguage
import io.github.reactivecircus.appversioning.fixtures.withFixtureRunner
import io.github.reactivecircus.appversioning.internal.GitClient
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateAppVersionInfoTest {
    @get:Rule
    val fixtureDir = TemporaryFolder()

    @Test
    fun `GenerateAppVersionInfo fails when root Gradle project is not a git root directory and gitRootDirectory is not provided`() {
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.FAILED, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertTrue(
                output.contains(
                    "Android App Versioning Gradle Plugin works with git tags but root project 'app-versioning-fixture' is not a git root directory, and a valid gitRootDirectory is not provided."
                )
            )
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when root Gradle project is not a git root directory and gitRootDirectory provided is not a git root directory`() {
        val extensions = """
            appVersioning {
                gitRootDirectory.set(rootDir.resolve("../"))
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions)),
            parentDirectoryName = "android"
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.FAILED, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertTrue(
                output.contains(
                    "Android App Versioning Gradle Plugin works with git tags but root project 'app-versioning-fixture' is not a git root directory, and a valid gitRootDirectory is not provided."
                )
            )
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when bareGitRepoDirectory provided is not a bare git repository`() {
        val extensions = """
            appVersioning {
                bareGitRepoDirectory.set(rootDir.resolve("../"))
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions)),
            parentDirectoryName = "android"
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.FAILED, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertTrue(
                output.contains(
                    "Android App Versioning Gradle Plugin works with git tags but root project 'app-versioning-fixture' is not a git root directory, and a valid gitRootDirectory is not provided."
                )
            )
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates versionCode and versionName when root Gradle project is not a git root directory but gitRootDirectory provided is a git root directory`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionCodeFile = File(fixtureDir.root, "android/app/build/outputs/app_versioning/release/version_code.txt")
        val versionNameFile = File(fixtureDir.root, "android/app/build/outputs/app_versioning/release/version_name.txt")

        val extensions = """
            appVersioning {
                gitRootDirectory.set(rootDir.resolve("../"))
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions)),
            parentDirectoryName = "android"
        ).runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals("10203", versionCodeFile.readText())
            assertEquals("1.2.3", versionNameFile.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates versionCode and versionName when bareGitRepoDirectory is provided`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionCodeFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt")
        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        val extensions = """
            appVersioning {
                gitRootDirectory.set(rootDir.resolve("non-existent"))
                bareGitRepoDirectory.set(rootDir.resolve(".git"))
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        ).runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("10203", versionCodeFile.readText())
            assertEquals("1.2.3", versionNameFile.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo fetches remote tags when fetchTagsWhenNoneExistsLocally is enabled and no local tags exist`() {
        val remoteFolder = TemporaryFolder().apply { create() }
        try {
            val remoteGitClient = GitClient.initialize(remoteFolder.root)
            val commitId = remoteGitClient.commit(message = "1st commit.")

            ProcessBuilder("git", "clone", "--no-tags", remoteFolder.root.absolutePath, fixtureDir.root.absolutePath)
                .start()
                .waitFor()

            remoteGitClient.tag(name = "1.2.3", message = "remote tag", commitId = commitId)

            val extension = """
                appVersioning {
                    fetchTagsWhenNoneExistsLocally.set(true)
                }
            """.trimIndent()

            withFixtureRunner(
                fixtureDir = fixtureDir,
                subprojects = listOf(AppProjectTemplate(pluginExtension = extension))
            ).runAndCheckResult(
                "generateAppVersionInfoForRelease"
            ) {
                assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
                assertTrue(output.contains("No git tags found. Fetching tags from remote."))
                assertEquals(
                    "10203",
                    File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt").readText()
                )
                assertEquals(
                    "1.2.3",
                    File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt").readText()
                )
            }
        } finally {
            remoteFolder.delete()
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates versionCode by converting latest SemVer-compliant git tag to a single integer using positional notation when no custom versionCode spec is provided`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3-alpha01+build.567", message = "1st tag", commitId = commitId)
        }

        val versionCodeFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt")

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals("10203", versionCodeFile.readText())
        }

        val commitId2 = gitClient.commit(message = "2nd commit.")
        gitClient.tag(name = "v3.0.0-SNAPSHOT", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals("30000", versionCodeFile.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when latest git tag is not SemVer-compliant and no custom versionCode spec is provided`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2", message = "1st tag", commitId = commitId)
        }

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.FAILED, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertTrue(
                output.contains(
                    """
                        Could not generate versionCode as "1.2" does not follow semantic versioning.
                        Please either ensure latest git tag follows semantic versioning, or provide a custom spec for generating versionCode using the `versionCode` property.
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when SemVer-compliant git tag has a component that exceeds the maximum digits allocated (2) and no custom versionCode spec is provided`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.100", message = "1st tag", commitId = commitId)
        }

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.FAILED, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertTrue(
                output.contains(
                    """
                        Could not generate versionCode from "1.2.100" as the SemVer cannot be represented as an Integer.
                        This is usually because MAJOR or MINOR version is greater than 99, as by default maximum of 2 digits is allowed for MINOR and PATCH components of a SemVer tag.
                        Another reason might be that the overall positional notation of the SemVer (MAJOR * 10000 + MINOR * 100 + PATCH) is greater than the maximum value of an integer (2147483647).
                        As a workaround you can provide a custom spec for generating versionCode using the `versionCode` property.
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates fallback versionCode and versionName when no git tags exist`() {
        GitClient.initialize(fixtureDir.root).apply {
            commit(message = "1st commit.")
        }

        val versionCodeFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt")
        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertTrue(
                output.contains(
                    """
                        No git tags found. Falling back to version code 0 and version name "".
                        If you want to fallback to the versionCode and versionName set via the DSL or manifest, or stop generating versionCode and versionName from Git tags:
                        appVersioning {
                            enabled.set(false)
                        }
                    """.trimIndent()
                )
            )
            assertEquals("0", versionCodeFile.readText())
            assertEquals("", versionNameFile.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates custom versionCode when custom versionCode spec is provided`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionCodeFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt")

        val extensions = """
            import io.github.reactivecircus.appversioning.VersionCodeSpec
            val buildNumber = providers.gradleProperty("buildNumber").map(String::toInt).orElse(0)
            appVersioning {
                versionCode.set(
                    buildNumber.map { buildNum ->
                        VersionCodeSpec {
                            tag.toSemVer().toVersionCode() * 100 + buildNum
                        }
                    }
                )
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        ).runAndCheckResult(
            "generateAppVersionInfoForRelease",
            "-PbuildNumber=78"
        ) {
            assertEquals("1020378", versionCodeFile.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when converting a non-SemVer compliant git tag to SemVer in a custom versionCode spec`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2", message = "1st tag", commitId = commitId)
        }

        val extensions = """
            appVersioning {
                versionCode.set {
                    tag.toSemVer().toVersionCode() * 100
                }
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.FAILED, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertTrue(output.contains("\"1.2\" is not a valid SemVer (https://semver.org/#semantic-versioning-200)."))
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates versionName directly from the latest git tag when no custom versionName spec is provided`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "0.0.1", message = "1st tag", commitId = commitId)
        }

        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        val extensions = """
            appVersioning {
                versionCode.set { 0 }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals("0.0.1", versionNameFile.readText())
        }

        val commitId2 = gitClient.commit(message = "2nd commit.")
        gitClient.tag(name = "v1.1.0-alpha01", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals("v1.1.0-alpha01", versionNameFile.readText())
        }

        val commitId3 = gitClient.commit(message = "3rd commit.")
        gitClient.tag(name = "alpha", message = "3rd tag", commitId = commitId3)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals("alpha", versionNameFile.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates custom versionName when custom versionName spec is provided`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        val extensions = $$"""
            appVersioning {
                versionName.set {
                    "Version $tag"
                }
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        ).runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals("Version 1.2.3", versionNameFile.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo can generate custom versionCode based on build variant`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionCodeFileForFreeDebug = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/freeDebug/version_code.txt"
        )
        val versionCodeFileForFreeRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/freeRelease/version_code.txt"
        )
        val versionCodeFileForPaidDebug = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/paidDebug/version_code.txt"
        )
        val versionCodeFileForPaidRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/paidRelease/version_code.txt"
        )

        val extensions = """
            appVersioning {
                versionCode.set {
                    val offset = if (variant.flavorName == "paid") 1 else 0
                    tag.toSemVer().toVersionCode() * 100 + offset
                }
            }
        """.trimIndent()

        val flavors = listOf("free", "paid")
        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions, flavors = flavors))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForFreeDebug"
        ) {
            assertEquals("1020300", versionCodeFileForFreeDebug.readText())
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForFreeRelease"
        ) {
            assertEquals("1020300", versionCodeFileForFreeRelease.readText())
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForPaidDebug"
        ) {
            assertEquals("1020301", versionCodeFileForPaidDebug.readText())
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForPaidRelease"
        ) {
            assertEquals("1020301", versionCodeFileForPaidRelease.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo can generate custom versionName based on build variant`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionNameFileForFreeDebug = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/freeDebug/version_name.txt"
        )
        val versionNameFileForFreeRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/freeRelease/version_name.txt"
        )
        val versionNameFileForPaidDebug = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/paidDebug/version_name.txt"
        )
        val versionNameFileForPaidRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/paidRelease/version_name.txt"
        )

        val extensions = $$"""
            appVersioning {
                versionName.set {
                    val suffix = if (!variant.isReleaseBuild) " (${variant.variantName})" else ""
                    "Version $tag$suffix"
                }
            }
        """.trimIndent()

        val flavors = listOf("free", "paid")
        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions, flavors = flavors))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForFreeDebug"
        ) {
            assertEquals("Version 1.2.3 (freeDebug)", versionNameFileForFreeDebug.readText())
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForFreeRelease"
        ) {
            assertEquals("Version 1.2.3", versionNameFileForFreeRelease.readText())
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForPaidDebug"
        ) {
            assertEquals("Version 1.2.3 (paidDebug)", versionNameFileForPaidDebug.readText())
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForPaidRelease"
        ) {
            assertEquals("Version 1.2.3", versionNameFileForPaidRelease.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates versionCode and versionName from the latest git tag matching the provided tagFilter pattern when a tagFilter pattern is provided`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            commit(message = "Initial commit.")
        }

        val versionCodeFileAppA = File(fixtureDir.root, "app-a/build/outputs/app_versioning/release/version_code.txt")
        val versionCodeFileAppB = File(fixtureDir.root, "app-b/build/outputs/app_versioning/release/version_code.txt")
        val versionCodeFileAppC = File(fixtureDir.root, "app-c/build/outputs/app_versioning/release/version_code.txt")
        val versionNameFileAppA = File(fixtureDir.root, "app-a/build/outputs/app_versioning/release/version_name.txt")
        val versionNameFileAppB = File(fixtureDir.root, "app-b/build/outputs/app_versioning/release/version_name.txt")
        val versionNameFileAppC = File(fixtureDir.root, "app-c/build/outputs/app_versioning/release/version_name.txt")

        val tagFilterAppA = "[0-9]*.[0-9]*.[0-9]*+appA"
        val tagFilterAppB = "[0-9]*.[0-9]*.[0-9]*+appB"
        val tagFilterAppC = "[0-9]*.[0-9]*.[0-9]*+appC"

        val extensionsAppA = """
            appVersioning {
                tagFilter.set("$tagFilterAppA")
            }
        """.trimIndent()

        val extensionsAppB = """
            appVersioning {
                tagFilter.set("$tagFilterAppB")
            }
        """.trimIndent()

        val extensionsAppC = """
            appVersioning {
                tagFilter.set("$tagFilterAppC")
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(
                AppProjectTemplate(
                    projectName = "app-a",
                    pluginExtension = extensionsAppA
                ),
                AppProjectTemplate(
                    projectName = "app-b",
                    pluginExtension = extensionsAppB
                ),
                AppProjectTemplate(
                    projectName = "app-c",
                    pluginExtension = extensionsAppC
                )
            )
        )

        // 1st appA release
        val commitId = gitClient.commit(message = "appA release.")
        gitClient.tag(name = "0.1.0+appA", message = "1st tag", commitId = commitId)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app-a:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("100", versionCodeFileAppA.readText())
            assertEquals("0.1.0+appA", versionNameFileAppA.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-b:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("0", versionCodeFileAppB.readText())
            assertEquals("", versionNameFileAppB.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-c:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("0", versionCodeFileAppC.readText())
            assertEquals("", versionNameFileAppC.readText())
        }

        gitClient.commit(message = "Commit a.")
        gitClient.commit(message = "Commit b.")

        // 1st appB release
        val commitId2 = gitClient.commit(message = "appB release.")
        gitClient.tag(name = "1.2.3-rc01+appB", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app-a:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("100", versionCodeFileAppA.readText())
            assertEquals("0.1.0+appA", versionNameFileAppA.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-b:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("10203", versionCodeFileAppB.readText())
            assertEquals("1.2.3-rc01+appB", versionNameFileAppB.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-c:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("0", versionCodeFileAppC.readText())
            assertEquals("", versionNameFileAppC.readText())
        }

        gitClient.commit(message = "Commit c.")

        // 2nd appB release
        val commitId3 = gitClient.commit(message = "appB release.")
        gitClient.tag(name = "1.2.3-rc02+appB", message = "3rd tag", commitId = commitId3)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app-a:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("100", versionCodeFileAppA.readText())
            assertEquals("0.1.0+appA", versionNameFileAppA.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-b:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("10203", versionCodeFileAppB.readText())
            assertEquals("1.2.3-rc02+appB", versionNameFileAppB.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-c:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("0", versionCodeFileAppC.readText())
            assertEquals("", versionNameFileAppC.readText())
        }

        // 1st appC release
        val commitId4 = gitClient.commit(message = "appC release.")
        gitClient.tag(name = "10.3.5-alpha03+appC", message = "4th tag", commitId = commitId4)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app-a:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("100", versionCodeFileAppA.readText())
            assertEquals("0.1.0+appA", versionNameFileAppA.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-b:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("10203", versionCodeFileAppB.readText())
            assertEquals("1.2.3-rc02+appB", versionNameFileAppB.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-c:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("100305", versionCodeFileAppC.readText())
            assertEquals("10.3.5-alpha03+appC", versionNameFileAppC.readText())
        }

        gitClient.commit(message = "Commit d.")

        // 2nd appA release
        val commitId5 = gitClient.commit(message = "appA release.")
        gitClient.tag(name = "0.2.1+appA", message = "4th tag", commitId = commitId5)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app-a:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("201", versionCodeFileAppA.readText())
            assertEquals("0.2.1+appA", versionNameFileAppA.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-b:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("10203", versionCodeFileAppB.readText())
            assertEquals("1.2.3-rc02+appB", versionNameFileAppB.readText())

            assertEquals(TaskOutcome.SUCCESS, task(":app-c:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("100305", versionCodeFileAppC.readText())
            assertEquals("10.3.5-alpha03+appC", versionNameFileAppC.readText())
        }
    }

    @Test
    fun `versionCode and versionName configurations work in groovy`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
            commit(message = "2nd commit.")
        }

        val versionCodeFileForDebug = File(fixtureDir.root, "app/build/outputs/app_versioning/debug/version_code.txt")
        val versionNameFileForDebug = File(fixtureDir.root, "app/build/outputs/app_versioning/debug/version_name.txt")
        val versionCodeFileForRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/release/version_code.txt"
        )
        val versionNameFileForRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/release/version_name.txt"
        )

        val extensions = $$"""
            appVersioning {
                versionCode {
                    tag.toSemVer().toVersionCode() + tag.commitsSinceLatestTag
                }
                versionName {
                    def suffix = variant.debugBuild ? " (${variant.variantName})" : ""
                    "Version ${tag.rawTagName}${suffix}"
                }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(
                AppProjectTemplate(
                    pluginExtension = extensions,
                    buildScriptLanguage = BuildScriptLanguage.Groovy,
                )
            )
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForDebug"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForDebug")?.outcome)
            assertEquals("10204", versionCodeFileForDebug.readText())
            assertEquals("Version 1.2.3 (debug)", versionNameFileForDebug.readText())
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("10204", versionCodeFileForRelease.readText())
            assertEquals("Version 1.2.3", versionNameFileForRelease.readText())
        }
    }

    @Test
    fun `versionCode and versionName configurations with external Provider work in groovy`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
            commit(message = "2nd commit.")
        }

        val versionCodeFileForRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/release/version_code.txt"
        )
        val versionNameFileForRelease = File(
            fixtureDir.root,
            "app/build/outputs/app_versioning/release/version_name.txt"
        )

        val extensions = $$"""
            import io.github.reactivecircus.appversioning.VersionCodeSpec

            def buildNumber = providers.gradleProperty("buildNumber").map { it.toInteger() }.orElse(0)

            appVersioning {
                versionCode.set(
                    buildNumber.map { buildNum ->
                        {
                            tag.toSemVer().toVersionCode() * 100 + buildNum
                        } as VersionCodeSpec
                    }
                )
                versionName {
                    "Version ${tag.rawTagName}"
                }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(
                AppProjectTemplate(
                    pluginExtension = extensions,
                    buildScriptLanguage = BuildScriptLanguage.Groovy,
                )
            )
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease",
            "-PbuildNumber=42"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
            assertEquals("1020342", versionCodeFileForRelease.readText())
            assertEquals("Version 1.2.3", versionNameFileForRelease.readText())
        }
    }

    @Test
    fun `GenerateAppVersionInfo is incremental without custom versionCode and versionName specs`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is incremental with custom versionCode and versionName specs`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
            commit(message = "2nd commit.")
        }

        val extensions = $$"""
            appVersioning {
                versionCode.set {
                    tag.toSemVer().toVersionCode() + tag.commitsSinceLatestTag
                }
                versionName.set {
                    "Version $tag"
                }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is cacheable without custom versionCode and versionName specs`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "clean",
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:clean")?.outcome)
            assertEquals(TaskOutcome.FROM_CACHE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is cacheable with custom versionCode and versionName specs`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
            commit(message = "2nd commit.")
        }

        val extensions = $$"""
            appVersioning {
                versionCode.set {
                    tag.toSemVer().toVersionCode() + tag.commitsSinceLatestTag
                }
                versionName.set {
                    "Version $tag"
                }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "clean",
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:clean")?.outcome)
            assertEquals(TaskOutcome.FROM_CACHE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is incremental for specific build variants`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val flavors = listOf("mock", "prod")
        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(flavors = flavors))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForProdRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForProdRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForMockRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForMockRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is cacheable for specific build variants`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val flavors = listOf("mock", "prod")
        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(flavors = flavors))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForProdRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForProdRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "clean",
            "generateAppVersionInfoForMockRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:clean")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForMockRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo (up-to-date) is re-executed after changing git refs`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        gitClient.commit(message = "2nd commit.")

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo (from cache) is re-executed after changing git refs`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }
        val commitId2 = gitClient.commit(message = "2nd commit.")

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "clean",
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:clean")?.outcome)
            assertEquals(TaskOutcome.FROM_CACHE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        gitClient.tag(name = "1.3.0", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "clean",
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo (up-to-date) is re-executed after changing git HEAD`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId1 = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId1)
            val commitId2 = commit(message = "2nd commit.")
            tag(name = "1.2.4", message = "2st tag", commitId = commitId2)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        gitClient.checkoutTag(tag = "1.2.3")

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }

    @Test
    fun `GenerateAppVersionInfo (from cache) is re-executed after changing git HEAD`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId1 = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId1)
            val commitId2 = commit(message = "2nd commit.")
            tag(name = "1.2.4", message = "2st tag", commitId = commitId2)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        runner.runAndCheckResult(
            "clean",
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:clean")?.outcome)
            assertEquals(TaskOutcome.FROM_CACHE, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }

        gitClient.checkoutTag(tag = "1.2.3")

        runner.runAndCheckResult(
            "clean",
            "generateAppVersionInfoForRelease",
            "--build-cache"
        ) {
            assertEquals(TaskOutcome.SUCCESS, task(":app:generateAppVersionInfoForRelease")?.outcome)
        }
    }
}

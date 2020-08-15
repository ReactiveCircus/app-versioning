@file:Suppress("FunctionName", "DuplicatedCode")

package io.github.reactivecircus.appversioning.tasks

import com.google.common.truth.Truth.assertThat
import io.github.reactivecircus.appversioning.fixtures.AppProjectTemplate
import io.github.reactivecircus.appversioning.fixtures.withFixtureRunner
import io.github.reactivecircus.appversioning.internal.GitClient
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GenerateAppVersionInfoTest {

    @get:Rule
    val fixtureDir = TemporaryFolder()

    @Test
    fun `GenerateAppVersionInfo fails when root project is not a git repository`() {
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Android App Versioning Gradle Plugin works with git tags but root project 'app-versioning-fixture' is not a valid git repository.")
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates versionCode by converting latest SemVer-compliant git tag to a single integer using positional notation when no custom versionCode generation rule is provided`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "0.0.1", message = "1st tag", commitId = commitId)
        }

        val versionCodeFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt")

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("1")
        }

        val commitId2 = gitClient.commit(message = "2nd commit.")
        gitClient.tag(name = "0.1.0", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("100")
        }

        val commitId3 = gitClient.commit(message = "3rd commit.")
        gitClient.tag(name = "1.0.0", message = "3rd tag", commitId = commitId3)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("10000")
        }

        val commitId4 = gitClient.commit(message = "4th commit.")
        gitClient.tag(name = "1.0.99", message = "4th tag", commitId = commitId4)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("10099")
        }

        val commitId5 = gitClient.commit(message = "5th commit.")
        gitClient.tag(name = "1.99.99", message = "5th tag", commitId = commitId5)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("19999")
        }

        val commitId6 = gitClient.commit(message = "6th commit.")
        gitClient.tag(name = "1.2.3-alpha01+build.567", message = "6th tag", commitId = commitId6)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("10203")
        }

        val commitId7 = gitClient.commit(message = "7th commit.")
        gitClient.tag(name = "v3.0.0-SNAPSHOT", message = "7th tag", commitId = commitId7)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("30000")
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when latest git tag is not SemVer-compliant and no custom versionCode generation rule is provided`() {
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
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains(
                """
                    Could not generate versionCode as "1.2" does not follow semantic versioning.
                    Please either ensure latest git tag follows semantic versioning, or provide a custom rule for generating versionCode using the `overrideVersionCode` lambda.
                """.trimIndent()
            )
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when SemVer-compliant git tag has a component that exceeds the maximum digits allocated (2) and no custom versionCode generation rule is provided`() {
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
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains(
                """
                    Could not generate versionCode from "1.2.100" as the SemVer cannot be represented as an Integer.
                    This is usually because MAJOR or MINOR version is greater than 99, as by default maximum of 2 digits is allowed for MINOR and PATCH components of a SemVer tag.
                    Another reason might be that the overall positional notation of the SemVer (MAJOR * 10000 + MINOR * 100 + PATCH) is greater than the maximum value of an integer (2147483647).
                    As a workaround you can provide a custom rule for generating versionCode using the `overrideVersionCode` lambda.
                """.trimIndent()
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
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    No git tags found. Falling back to version code ${GenerateAppVersionInfo.VERSION_CODE_FALLBACK} and version name "${GenerateAppVersionInfo.VERSION_NAME_FALLBACK}".
                    If you want to fallback to the versionCode and versionName set via the DSL or manifest, or stop generating versionCode and versionName from Git tags:
                    appVersioning {
                        enabled.set(false)
                    }
                """.trimIndent()
            )
            assertThat(versionCodeFile.readText()).isEqualTo(GenerateAppVersionInfo.VERSION_CODE_FALLBACK.toString())
            assertThat(versionNameFile.readText()).isEqualTo(GenerateAppVersionInfo.VERSION_NAME_FALLBACK)
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates custom versionCode when custom versionCode generation rule is provided`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionCodeFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt")

        val extensions = """
            import io.github.reactivecircus.appversioning.toSemVer
            appVersioning {
                overrideVersionCode { gitTag, providers ->
                    val buildNumber = providers
                        .gradleProperty("buildNumber")
                        .orNull?.toInt()?: 0
                    val semVer = gitTag.toSemVer()
                    semVer.major * 1000000 + semVer.minor * 10000 + semVer.patch * 100 + buildNumber
                }
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        ).runAndCheckResult(
            "generateAppVersionInfoForRelease", "-PbuildNumber=78"
        ) {
            assertThat(versionCodeFile.readText()).isEqualTo("1020378")
        }
    }

    @Test
    fun `GenerateAppVersionInfo fails when converting a non-SemVer compliant git tag to SemVer in a custom versionCode generation rule`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2", message = "1st tag", commitId = commitId)
        }

        val extensions = """
            import io.github.reactivecircus.appversioning.toSemVer
            appVersioning {
                overrideVersionCode { gitTag, providers ->
                    val semVer = gitTag.toSemVer()
                    semVer.major * 1000000 + semVer.minor * 10000 + semVer.patch * 100
                }
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        ).runAndExpectFailure(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("\"1.2\" is not a valid SemVer.")
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates versionName directly from the latest git tag when no custom versionName generation rule is provided`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "0.0.1", message = "1st tag", commitId = commitId)
        }

        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        val extensions = """
            appVersioning {
                overrideVersionCode { _, _ -> 0 }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionNameFile.readText()).isEqualTo("0.0.1")
        }

        val commitId2 = gitClient.commit(message = "2nd commit.")
        gitClient.tag(name = "v1.1.0-alpha01", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionNameFile.readText()).isEqualTo("v1.1.0-alpha01")
        }

        val commitId3 = gitClient.commit(message = "3rd commit.")
        gitClient.tag(name = "alpha", message = "3rd tag", commitId = commitId3)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionNameFile.readText()).isEqualTo("alpha")
        }
    }

    @Test
    fun `GenerateAppVersionInfo generates custom versionName when custom versionName generation rule is provided`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        val extensions = """
            appVersioning {
                overrideVersionName { gitTag, _ ->
                    "Version " + gitTag.toString()
                }
            }
        """.trimIndent()

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        ).runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionNameFile.readText()).isEqualTo("Version 1.2.3")
        }
    }

    @Test
    fun `overrideVersionCode and overrideVersionName configurations work in groovy`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
            commit(message = "2nd commit.")
        }

        val versionCodeFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt")
        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        val extensions = """
            import io.github.reactivecircus.appversioning.SemVer
            appVersioning {
                overrideVersionCode { gitTag, _ ->
                    def semVer = SemVer.fromGitTag(gitTag)
                    semVer.major * 10000 + semVer.minor * 100 + semVer.patch + gitTag.commitsSinceLatestTag
                }
                overrideVersionName { gitTag, _ ->
                    "Version " + gitTag.toString()
                }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions, useKts = false))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(versionCodeFile.readText()).isEqualTo("10204")
            assertThat(versionNameFile.readText()).isEqualTo("Version 1.2.3")
        }
    }

    @Test
    fun `GenerateAppVersionInfo is incremental without custom versionCode and versionName generation rules`() {
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
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is incremental with custom versionCode and versionName generation rules`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
            commit(message = "2nd commit.")
        }

        val extensions = """
            import io.github.reactivecircus.appversioning.toSemVer
            appVersioning {
                overrideVersionCode { gitTag, _ ->
                    val semVer = gitTag.toSemVer()
                    semVer.major * 10000 + semVer.minor * 100 + semVer.patch + gitTag.commitsSinceLatestTag
                }
                overrideVersionName { gitTag, _ ->
                    "Version " + gitTag.toString()
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
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is cacheable without custom versionCode and versionName generation rules`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "clean", "generateAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:clean")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
        }
    }

    @Test
    fun `GenerateAppVersionInfo is cacheable with custom versionCode and versionName generation rules`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
            commit(message = "2nd commit.")
        }

        val extensions = """
            import io.github.reactivecircus.appversioning.toSemVer
            appVersioning {
                overrideVersionCode { gitTag, _ ->
                    val semVer = gitTag.toSemVer()
                    semVer.major * 10000 + semVer.minor * 100 + semVer.patch + gitTag.commitsSinceLatestTag
                }
                overrideVersionName { gitTag, _ ->
                    "Version " + gitTag.toString()
                }
            }
        """.trimIndent()

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extensions))
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "clean", "generateAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:clean")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
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
            assertThat(task(":app:generateAppVersionInfoForProdRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForMockRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForMockRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
            "generateAppVersionInfoForProdRelease", "--build-cache"
        ) {
            assertThat(task(":app:generateAppVersionInfoForProdRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "clean", "generateAppVersionInfoForMockRelease", "--build-cache"
        ) {
            assertThat(task(":app:clean")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":app:generateAppVersionInfoForMockRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }

        gitClient.commit(message = "2nd commit.")

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
            "generateAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "clean", "generateAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:clean")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
        }

        gitClient.tag(name = "1.3.0", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }
}

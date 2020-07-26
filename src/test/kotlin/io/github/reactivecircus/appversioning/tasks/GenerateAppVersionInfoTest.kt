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
    fun `GenerateAppVersionInfo generates versionCode by converting latest SemVer tag to integer when no custom versionCode generation rule is provided`() {
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
    }

    @Test
    fun `GenerateAppVersionInfo generates versionName directly from the latest SemVer tag when no custom versionName generation rule is provided`() {
        val gitClient = GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "0.0.1", message = "1st tag", commitId = commitId)
        }

        val versionNameFile = File(fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt")

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionNameFile.readText()).isEqualTo("0.0.1")
        }

        val commitId2 = gitClient.commit(message = "2nd commit.")
        gitClient.tag(name = "0.1.0", message = "2nd tag", commitId = commitId2)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionNameFile.readText()).isEqualTo("0.1.0")
        }

        val commitId3 = gitClient.commit(message = "3rd commit.")
        gitClient.tag(name = "1.0.0", message = "3rd tag", commitId = commitId3)

        runner.runAndCheckResult(
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(versionNameFile.readText()).isEqualTo("1.0.0")
        }
    }

    // TODO test with `requireValidGitTag`, `overrideVersionCode`, `overrideVersionName`
    // TODO test groovy plugin configuration in build.gradle

    @Test
    fun `GenerateAppVersionInfo is incremental`() {
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
    fun `GenerateAppVersionInfo is cacheable`() {
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
}

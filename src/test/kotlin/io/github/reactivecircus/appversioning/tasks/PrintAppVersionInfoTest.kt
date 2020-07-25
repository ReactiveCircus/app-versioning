package io.github.reactivecircus.appversioning.tasks

import com.google.common.truth.Truth.assertThat
import io.github.reactivecircus.appversioning.fixtures.AppProjectTemplate
import io.github.reactivecircus.appversioning.fixtures.withFixtureRunner
import io.github.reactivecircus.appversioning.internal.GitClient
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrintAppVersionInfoTest {

    @get:Rule
    val fixtureDir = TemporaryFolder()

    @Test
    fun `PrintAppVersionInfo prints versionCode and versionName to the console`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndCheckResult(
            "printAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    versionCode: 10203
                    versionName: 1.2.3
                """.trimIndent()
            )
        }
    }

    @Test
    fun `PrintAppVersionInfo is not incremental`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "printAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    versionCode: 10203
                    versionName: 1.2.3
                """.trimIndent()
            )
        }

        runner.runAndCheckResult(
            "printAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    versionCode: 10203
                    versionName: 1.2.3
                """.trimIndent()
            )
        }
    }

    @Test
    fun `PrintAppVersionInfo is not cacheable`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        val runner = withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        )

        runner.runAndCheckResult(
            "printAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    versionCode: 10203
                    versionName: 1.2.3
                """.trimIndent()
            )
        }

        runner.runAndCheckResult(
            "clean", "printAppVersionInfoForRelease", "--build-cache"
        ) {
            assertThat(task(":app:clean")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":app:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    versionCode: 10203
                    versionName: 1.2.3
                """.trimIndent()
            )
        }
    }
}

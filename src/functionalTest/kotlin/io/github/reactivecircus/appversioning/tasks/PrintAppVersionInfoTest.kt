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

class PrintAppVersionInfoTest {

    @get:Rule
    val fixtureDir = TemporaryFolder()

    @Test
    fun `PrintAppVersionInfo prints latest generated versionCode and versionName to the console when they are available`() {
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
            "printAppVersionInfoForProdRelease"
        ) {
            assertThat(task(":app:printAppVersionInfoForProdRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    App version info generated by Android App Versioning plugin:
                    Project: ":app"
                    Build variant: prodRelease
                    versionCode: 10203
                    versionName: "1.2.3"
                """.trimIndent()
            )
        }
    }

    @Test
    fun `PrintAppVersionInfo prints warning message to the console when they are not available`() {
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
                "No app version info (versionCode and versionName) generated by the Android App Versioning plugin for the \"release\" variant of project \":app\" is available."
            )
        }
    }

    @Test
    fun `PrintAppVersionInfo prints latest generate versionCode and versionName (or warning message if not available) to the console for each project with AppVersioningPlugin applied`() {
        GitClient.initialize(fixtureDir.root).apply {
            commit(message = "Initial commit.")
            // appA release
            val commitId1 = commit(message = "appA release.")
            tag(name = "0.1.0+appA", message = "1st tag", commitId = commitId1)
            // appB release
            val commitId2 = commit(message = "appB release.")
            tag(name = "1.2.3+appB", message = "2nd tag", commitId = commitId2)
            // appC release
            val commitId4 = commit(message = "appC release.")
            tag(name = "10.3.5+appC", message = "4th tag", commitId = commitId4)
        }

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

        runner.runAndCheckResult(
            "app-a:generateAppVersionInfoForRelease",
            "app-b:generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app-a:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":app-b:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "printAppVersionInfoForRelease"
        ) {
            assertThat(task(":app-a:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    App version info generated by Android App Versioning plugin:
                    Project: ":app-a"
                    Build variant: release
                    versionCode: 100
                    versionName: "0.1.0+appA"
                """.trimIndent()
            )

            assertThat(task(":app-b:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    App version info generated by Android App Versioning plugin:
                    Project: ":app-b"
                    Build variant: release
                    versionCode: 10203
                    versionName: "1.2.3+appB"
                """.trimIndent()
            )

            assertThat(task(":app-a:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                "No app version info (versionCode and versionName) generated by the Android App Versioning plugin for the \"release\" variant of project \":app-c\" is available."
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
            "generateAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        runner.runAndCheckResult(
            "printAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    App version info generated by Android App Versioning plugin:
                    Project: ":app"
                    Build variant: release
                    versionCode: 10203
                    versionName: "1.2.3"
                """.trimIndent()
            )
        }

        runner.runAndCheckResult(
            "printAppVersionInfoForRelease"
        ) {
            assertThat(task(":app:printAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains(
                """
                    App version info generated by Android App Versioning plugin:
                    Project: ":app"
                    Build variant: release
                    versionCode: 10203
                    versionName: "1.2.3"
                """.trimIndent()
            )
        }
    }
}

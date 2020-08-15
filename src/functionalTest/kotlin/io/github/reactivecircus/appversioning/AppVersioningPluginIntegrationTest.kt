@file:Suppress("FunctionName")

package io.github.reactivecircus.appversioning

import com.google.common.truth.Truth.assertThat
import io.github.reactivecircus.appversioning.fixtures.AppProjectTemplate
import io.github.reactivecircus.appversioning.fixtures.LibraryProjectTemplate
import io.github.reactivecircus.appversioning.fixtures.withFixtureRunner
import io.github.reactivecircus.appversioning.internal.GitClient
import io.github.reactivecircus.appversioning.tasks.GenerateAppVersionInfo
import io.github.reactivecircus.appversioning.tasks.PrintAppVersionInfo
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppVersioningPluginIntegrationTest {

    @get:Rule
    val fixtureDir = TemporaryFolder()

    @Test
    fun `plugin cannot be applied to project without Android App plugin`() {
        GitClient.initialize(fixtureDir.root)

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(LibraryProjectTemplate())
        ).runAndExpectFailure(
            "help"
        ) {
            assertThat(task("help")?.outcome).isNull()
            assertThat(output).contains(
                "Android App Versioning plugin should only be applied to an Android Application project but project ':library' doesn't have the 'com.android.application' plugin applied."
            )
        }
    }

    @Test
    fun `plugin tasks are registered for Android App project without product flavors`() {
        GitClient.initialize(fixtureDir.root)

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndCheckResult(
            "tasks", "--group=versioning"
        ) {
            assertThat(output).contains("Versioning tasks")
            assertThat(output).contains("generateAppVersionInfoForRelease - ${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the release variant.")
            assertThat(output).contains("printAppVersionInfoForRelease - ${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the release variant.")
        }
    }

    @Test
    fun `plugin tasks are registered for Android App project with product flavors`() {
        GitClient.initialize(fixtureDir.root)

        val flavors = listOf("mock", "prod")
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(flavors = flavors))
        ).runAndCheckResult(
            "tasks", "--group=versioning"
        ) {
            assertThat(output).contains("Versioning tasks")
            assertThat(output).contains("generateAppVersionInfoForMockRelease - ${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockRelease variant.")
            assertThat(output).contains("printAppVersionInfoForMockRelease - ${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the mockRelease variant.")
            assertThat(output).contains("generateAppVersionInfoForProdRelease - ${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodRelease variant.")
            assertThat(output).contains("printAppVersionInfoForProdRelease - ${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the prodRelease variant.")
        }
    }

    @Test
    fun `plugin tasks are not registered for debug builds when releaseBuildOnly is enabled`() {
        GitClient.initialize(fixtureDir.root)

        val extension = """
            appVersioning {
                releaseBuildOnly.set(true)
            }
        """.trimIndent()
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extension))
        ).runAndCheckResult(
            "tasks", "--group=versioning"
        ) {
            assertThat(output).doesNotContain("generateAppVersionInfoForDebug")
            assertThat(output).doesNotContain("printAppVersionInfoForDebug")
        }
    }

    @Test
    fun `plugin tasks are registered for debug builds when releaseBuildOnly is disabled`() {
        GitClient.initialize(fixtureDir.root)

        val extension = """
            appVersioning {
                releaseBuildOnly.set(false)
            }
        """.trimIndent()
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extension))
        ).runAndCheckResult(
            "tasks", "--group=versioning"
        ) {
            assertThat(output).contains("generateAppVersionInfoForDebug - ${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the debug variant.")
            assertThat(output).contains("printAppVersionInfoForDebug - ${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the debug variant.")
        }
    }

    @Test
    fun `plugin tasks are registered when plugin is enabled`() {
        GitClient.initialize(fixtureDir.root)

        val extension = """
            appVersioning {
                enabled.set(true)
            }
        """.trimIndent()
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extension))
        ).runAndCheckResult(
            "tasks", "--group=versioning"
        ) {
            assertThat(output).contains("Versioning tasks")
            assertThat(output).contains("generateAppVersionInfoForRelease - ${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the release variant.")
            assertThat(output).contains("printAppVersionInfoForRelease - ${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the release variant.")
        }
    }

    @Test
    fun `plugin tasks are not registered when plugin is disabled`() {
        GitClient.initialize(fixtureDir.root)

        val extension = """
            appVersioning {
                enabled.set(false)
            }
        """.trimIndent()
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extension))
        ).runAndCheckResult(
            "tasks", "--group=versioning"
        ) {
            assertThat(output).doesNotContain("Versioning tasks")
            assertThat(output).contains("No tasks")
            assertThat(output).contains("Android App Versioning plugin is disabled.")
        }
    }

    @Test
    fun `plugin generates versionCode and versionName for the assembled APK when assemble task is run`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.2.3", message = "1st tag", commitId = commitId)
        }

        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate())
        ).runAndCheckResult(
            "assembleRelease"
        ) {
            val versionCodeFileContent = File(
                fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt"
            ).readText()
            val versionNameFileContent = File(
                fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt"
            ).readText()

            assertThat(task(":app:generateAppVersionInfoForRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":app:assembleRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            assertThat(output).contains("Generated app version code: 10203.")
            assertThat(output).contains("Generated app version name: \"1.2.3\".")

            assertThat(versionCodeFileContent).isEqualTo("10203")
            assertThat(versionNameFileContent).isEqualTo("1.2.3")
        }
    }

    @Test
    fun `plugin (when disabled) does not generate versionCode and versionName from git tag for the assembled APK when assemble task is run`() {
        GitClient.initialize(fixtureDir.root).apply {
            val commitId = commit(message = "1st commit.")
            tag(name = "1.0.0", message = "1st tag", commitId = commitId)
        }

        val extension = """
            appVersioning {
                enabled.set(false)
            }
        """.trimIndent()
        withFixtureRunner(
            fixtureDir = fixtureDir,
            subprojects = listOf(AppProjectTemplate(pluginExtension = extension))
        ).runAndCheckResult(
            "assembleRelease"
        ) {
            val versionCodeFile = File(
                fixtureDir.root, "app/build/outputs/app_versioning/release/version_code.txt"
            )
            val versionNameFile = File(
                fixtureDir.root, "app/build/outputs/app_versioning/release/version_name.txt"
            )

            assertThat(task(":app:assembleRelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            assertThat(output).doesNotContain("Generated app version code")
            assertThat(output).doesNotContain("Generated app version name")

            assertThat(versionCodeFile.exists()).isFalse()
            assertThat(versionNameFile.exists()).isFalse()
        }
    }
}

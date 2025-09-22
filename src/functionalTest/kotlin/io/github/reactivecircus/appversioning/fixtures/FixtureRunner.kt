package io.github.reactivecircus.appversioning.fixtures

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.rules.TemporaryFolder
import java.io.File

fun withFixtureRunner(
    fixtureDir: TemporaryFolder,
    subprojects: List<AndroidProjectTemplate>,
    parentDirectoryName: String? = null,
    dryRun: Boolean = false
) = FixtureRunner(
    fixtureDir = fixtureDir,
    subprojects = subprojects,
    parentDirectoryName = parentDirectoryName,
    dryRun = dryRun
)

class FixtureRunner(
    fixtureDir: TemporaryFolder,
    subprojects: List<AndroidProjectTemplate>,
    parentDirectoryName: String?,
    private val dryRun: Boolean
) {
    private val gradleRoot: File = if (parentDirectoryName.isNullOrBlank()) {
        fixtureDir.root
    } else {
        fixtureDir.root.resolve(parentDirectoryName)
    }

    init {
        fixtureDir.buildFixture(gradleRoot, subprojects)
    }

    fun runAndCheckResult(vararg commands: String, action: BuildResult.() -> Unit) {
        val buildResult = runner.withProjectDir(gradleRoot)
            .withArguments(buildArguments(commands.toList()))
            .withPluginClasspath()
            .withGradleVersion(GradleVersion.current().version)
            .build()
        action(buildResult)
    }

    fun runAndExpectFailure(vararg commands: String, action: BuildResult.() -> Unit) {
        val buildResult = runner.withProjectDir(gradleRoot)
            .withArguments(buildArguments(commands.toList()))
            .withPluginClasspath()
            .withGradleVersion(GradleVersion.current().version)
            .buildAndFail()
        action(buildResult)
    }

    private fun buildArguments(commands: List<String>): List<String> {
        val args = mutableListOf("--stacktrace", "--info")
        if (dryRun) {
            args.add("--dry-run")
        }
        args.addAll(commands)
        return args
    }
}

private val runner = GradleRunner.create()
    .withPluginClasspath()
    .withDebug(true)

private fun TemporaryFolder.buildFixture(gradleRoot: File, subprojects: List<AndroidProjectTemplate>) {
    // settings.gradle
    gradleRoot.resolve("settings.gradle.kts").also { it.parentFile.mkdirs() }
        .writeText(
            settingsFileContent(
                localBuildCacheUri = newFolder("local-cache").toURI().toString(),
                subprojects = subprojects
            )
        )

    // gradle.properties
    gradleRoot.resolve("gradle.properties").also { it.parentFile.mkdir() }
        .writeText(gradlePropertiesFileContent(enableConfigurationCache = false))

    // subprojects
    subprojects.forEach { subproject ->
        // build.gradle or build.gradle.kts
        val buildFileName = if (subproject.buildScriptLanguage == BuildScriptLanguage.Kts) {
            "build.gradle.kts"
        } else {
            "build.gradle"
        }
        gradleRoot.resolve("${subproject.projectName}/$buildFileName").also { it.parentFile.mkdirs() }
            .writeText(subproject.buildFileContent)

        // AndroidManifest.xml
        gradleRoot.resolve("${subproject.projectName}/src/main/AndroidManifest.xml").also { it.parentFile.mkdirs() }
            .writeText(subproject.manifestFileContent)
    }
}

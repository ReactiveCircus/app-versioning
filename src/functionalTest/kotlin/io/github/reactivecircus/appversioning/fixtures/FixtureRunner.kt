package io.github.reactivecircus.appversioning.fixtures

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.rules.TemporaryFolder

fun withFixtureRunner(
    fixtureDir: TemporaryFolder,
    subprojects: List<AndroidProjectTemplate>,
    dryRun: Boolean = false
) = FixtureRunner(
    fixtureDir = fixtureDir,
    subprojects = subprojects,
    dryRun = dryRun
)

class FixtureRunner(
    private val fixtureDir: TemporaryFolder,
    subprojects: List<AndroidProjectTemplate>,
    private val dryRun: Boolean
) {
    init {
        fixtureDir.buildFixture(subprojects)
    }

    fun runAndCheckResult(vararg commands: String, action: BuildResult.() -> Unit) {
        val buildResult = runner.withProjectDir(fixtureDir.root)
            .withArguments(buildArguments(commands.toList()))
            .withPluginClasspath()
            .withGradleVersion(GradleVersion.current().version)
            .build()
        action(buildResult)
    }

    fun runAndExpectFailure(vararg commands: String, action: BuildResult.() -> Unit) {
        val buildResult = runner.withProjectDir(fixtureDir.root)
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

private val runner = GradleRunner.create().withPluginClasspath()

private fun TemporaryFolder.buildFixture(subprojects: List<AndroidProjectTemplate>) {
    // build.gradle
    root.resolve("build.gradle").also { it.parentFile.mkdirs() }
        .writeText(rootBuildFileContent)

    // settings.gradle
    root.resolve("settings.gradle").also { it.parentFile.mkdirs() }
        .writeText(
            settingsFileContent(
                localBuildCacheUri = newFolder("local-cache").toURI().toString(),
                subprojects = subprojects
            )
        )

    // gradle.properties
    root.resolve("gradle.properties").also { it.parentFile.mkdir() }
        .writeText(gradlePropertiesFileContent(enableConfigurationCache = false))

    // subprojects
    subprojects.forEach { subproject ->
        // build.gradle or build.gradle.kts
        val buildFileName = if (subproject.useKts) "build.gradle.kts" else "build.gradle"
        root.resolve("${subproject.projectName}/$buildFileName").also { it.parentFile.mkdirs() }
            .writeText(subproject.buildFileContent)

        // AndroidManifest.xml
        root.resolve("${subproject.projectName}/src/main/AndroidManifest.xml").also { it.parentFile.mkdirs() }
            .writeText(subproject.manifestFileContent)
    }
}

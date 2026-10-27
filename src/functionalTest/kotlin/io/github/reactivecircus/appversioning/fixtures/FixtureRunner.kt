package io.github.reactivecircus.appversioning.fixtures

import com.autonomousapps.kit.GradleBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.junit.rules.TemporaryFolder
import java.io.File

fun withFixtureRunner(
    fixtureDir: TemporaryFolder,
    subprojects: List<AndroidProjectTemplate>,
    parentDirectoryName: String? = null,
    dryRun: Boolean = false,
): FixtureRunner {
    val project = createFixtureProject(fixtureDir.root, subprojects, parentDirectoryName)
    project.write()
    return FixtureRunner(project.rootDir, dryRun)
}

class FixtureRunner(
    private val gradleRoot: File,
    private val dryRun: Boolean,
) {
    fun runAndCheckResult(vararg commands: String, action: BuildResult.() -> Unit) {
        val buildResult = runner(commands.toList()).build()
        action(buildResult)
    }

    fun runAndExpectFailure(vararg commands: String, action: BuildResult.() -> Unit) {
        val buildResult = runner(commands.toList()).buildAndFail()
        action(buildResult)
    }

    private fun runner(commands: List<String>) = GradleBuilder
        .runner(GradleVersion.current(), gradleRoot, *buildArguments(commands).toTypedArray())
        .withDebug(true)

    private fun buildArguments(commands: List<String>): List<String> {
        val args = mutableListOf("--stacktrace", "--info")
        if (dryRun) {
            args.add("--dry-run")
        }
        args.addAll(commands)
        return args
    }
}

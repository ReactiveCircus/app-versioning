package io.github.reactivecircus.appversioning.fixtures

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.GradleProperties
import java.io.File

internal fun createFixtureProject(
    fixtureDir: File,
    subprojects: List<AndroidProjectTemplate>,
    parentDirectoryName: String?,
): GradleProject {
    val buildScriptLanguage = subprojects.firstOrNull()?.buildScriptLanguage ?: BuildScriptLanguage.Kts
    require(subprojects.all { it.buildScriptLanguage == buildScriptLanguage }) {
        "All subprojects in a fixture must use the same build script language."
    }
    val gradleRoot = if (parentDirectoryName.isNullOrBlank()) {
        fixtureDir
    } else {
        fixtureDir.resolve(parentDirectoryName)
    }
    val fixtureAgpVersion = checkNotNull(System.getProperty("fixtureAgpVersion"))
    val localBuildCacheUri = fixtureDir.resolve("local-cache").apply { mkdirs() }.toURI().toString()
    val enableConfigurationCache = currentAgpSupportsConfigurationCache()
    return GradleProject.Builder(gradleRoot, buildScriptLanguage.dslKind).apply {
        withRootProject {
            gradleProperties = GradleProperties.of(
                "org.gradle.parallel=true",
                "org.gradle.configureondemand=true",
                "org.gradle.caching=true",
                "org.gradle.configuration-cache=$enableConfigurationCache",
                "org.gradle.isolated-projects=$enableConfigurationCache",
            )
            withSettingsScript {
                rootProjectName = "app-versioning-fixture"
                additions = """
                    |buildCache {
                    |    local {
                    |        directory = "$localBuildCacheUri"
                    |    }
                    |}
                """.trimMargin()
            }
        }
        subprojects.forEach { subproject ->
            withSubproject(subproject.projectName) {
                buildScript = subproject.buildScript(fixtureAgpVersion)
                withFile("src/main/AndroidManifest.xml", subproject.manifestFileContent)
            }
        }
    }.build()
}

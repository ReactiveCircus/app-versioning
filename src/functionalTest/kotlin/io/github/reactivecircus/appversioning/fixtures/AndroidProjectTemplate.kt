package io.github.reactivecircus.appversioning.fixtures

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.gradle.BuildScript
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.Plugin

sealed class AndroidProjectTemplate {
    abstract val projectName: String
    abstract val pluginExtension: String?
    abstract val buildScriptLanguage: BuildScriptLanguage
    abstract val flavors: List<String>
    abstract val imports: List<String>

    internal fun buildScript(fixtureAgpVersion: String): BuildScript {
        val androidPluginId = if (this is AppProjectTemplate) "com.android.application" else "com.android.library"
        return BuildScript.Builder().apply {
            if (this@AndroidProjectTemplate.imports.isNotEmpty()) {
                imports = Imports.of(this@AndroidProjectTemplate.imports)
            }
            plugins(
                Plugin(androidPluginId, fixtureAgpVersion),
                AppVersioningPlugin,
            )
            additions = androidBlock() + "\n\n" + pluginExtension.orEmpty()
        }.build()
    }

    private fun androidBlock(): String = when (buildScriptLanguage) {
        BuildScriptLanguage.Kts -> kotlinAndroidBlock()
        BuildScriptLanguage.Groovy -> groovyAndroidBlock()
    }

    private fun kotlinAndroidBlock(): String {
        val flavorConfigs = if (flavors.isNotEmpty()) {
            """
                |flavorDimensions.add("environment")
                |productFlavors {
                |    ${flavors.joinToString("\n") { "register(\"$it\") {}" }}
                |}
            """.trimMargin()
        } else {
            ""
        }
        val abiConfigs = if (this is AppProjectTemplate && splitsApks) {
            """
                |splits {
                |    abi {
                |        isEnable = true
                |        reset()
                |        include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                |        isUniversalApk = $universalApk
                |    }
                |}
            """.trimMargin()
        } else {
            ""
        }
        return """
            |android {
            |    namespace = "$DEFAULT_PACKAGE_NAME.${projectName.replace("-", ".")}"
            |    compileSdk = 36
            |    defaultConfig {
            |        minSdk = 21
            |        ${if (this is AppProjectTemplate) "targetSdk = 36" else ""}
            |    }
            |    lint.checkReleaseBuilds = false
            |
            |    $flavorConfigs
            |
            |    $abiConfigs
            |}
        """.trimMargin()
    }

    private fun groovyAndroidBlock(): String {
        val flavorConfigs = if (flavors.isNotEmpty()) {
            """
                |flavorDimensions "environment"
                |productFlavors {
                |    ${flavors.joinToString("\n") { "$it {}" }}
                |}
            """.trimMargin()
        } else {
            ""
        }
        val abiConfigs = if (this is AppProjectTemplate && splitsApks) {
            """
                |splits {
                |    abi {
                |        enable true
                |        reset()
                |        include "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
                |        universalApk $universalApk
                |    }
                |}
            """.trimMargin()
        } else {
            ""
        }
        return """
            |android {
            |    namespace '$DEFAULT_PACKAGE_NAME.${projectName.replace("-", ".")}'
            |    compileSdk 36
            |    defaultConfig {
            |        minSdk 21
            |        ${if (this is AppProjectTemplate) "targetSdk 36" else ""}
            |    }
            |    lint {
            |        checkReleaseBuilds false
            |    }
            |
            |    $flavorConfigs
            |
            |    $abiConfigs
            |}
        """.trimMargin()
    }

    val manifestFileContent: String
        get() = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<manifest />
        """.trimMargin()

    companion object {
        private const val DEFAULT_PACKAGE_NAME = "io.github.reactivecircus.appversioning"
    }
}

private val AppVersioningPlugin = Plugin(
    id = "io.github.reactivecircus.app-versioning",
    version = AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION,
)

class AppProjectTemplate(
    override val projectName: String = "app",
    override val pluginExtension: String? = null,
    override val buildScriptLanguage: BuildScriptLanguage = BuildScriptLanguage.Kts,
    override val flavors: List<String> = emptyList(),
    val splitsApks: Boolean = false,
    val universalApk: Boolean = false,
    override val imports: List<String> = emptyList(),
) : AndroidProjectTemplate()

class LibraryProjectTemplate(
    override val projectName: String = "library",
    override val pluginExtension: String? = null,
    override val buildScriptLanguage: BuildScriptLanguage = BuildScriptLanguage.Kts,
    override val flavors: List<String> = emptyList(),
    override val imports: List<String> = emptyList(),
) : AndroidProjectTemplate()

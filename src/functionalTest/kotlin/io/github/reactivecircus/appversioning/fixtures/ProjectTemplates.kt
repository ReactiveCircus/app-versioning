package io.github.reactivecircus.appversioning.fixtures

val rootBuildFileContent = """
    allprojects {
        repositories {
            google()
        }
    }
""".trimIndent()

fun settingsFileContent(
    localBuildCacheUri: String,
    subprojects: List<AndroidProjectTemplate>
) = """
    buildCache {
        local {
            directory '$localBuildCacheUri'
        }
    }

    rootProject.name='app-versioning-fixture'

    ${subprojects.map { it.projectName }.joinToString("\n") { "include ':$it'" }}
""".trimIndent()

fun gradlePropertiesFileContent(enableConfigurationCache: Boolean): String {
    val configurationCacheProperties = if (enableConfigurationCache) {
        """
            org.gradle.unsafe.configuration-cache=true
        """.trimIndent()
    } else ""
    return """
        $configurationCacheProperties
    """.trimIndent()
}

abstract class AndroidProjectTemplate {
    abstract val projectName: String
    abstract val pluginExtension: String?
    abstract val useKts: Boolean
    abstract val flavors: List<String>
    abstract val isAppProject: Boolean

    val buildFileContent: String get() = if (useKts) ktsBuildFileContent else groovyBuildFileContent

    private val ktsBuildFileContent: String
        get() {
            val flavorConfigs = if (flavors.isNotEmpty()) """
                flavorDimensions("environment")
                productFlavors {
                    ${flavors.joinToString("\n") { "register(\"$it\") {}" }}
                }
            """.trimIndent() else ""
            return """
                plugins {
                    id("com.android.${if (isAppProject) "application" else "library"}")
                    id("io.github.reactivecircus.app-versioning")
                }

                ${pluginExtension ?: ""}

                android {
                    compileSdkVersion(31)
                    defaultConfig {
                        minSdkVersion(21)
                        targetSdkVersion(31)
                    }

                    lintOptions.isCheckReleaseBuilds = false

                    $flavorConfigs
                }
            """.trimIndent()
        }

    private val groovyBuildFileContent: String
        get() {
            val flavorConfigs = if (flavors.isNotEmpty()) """
                flavorDimensions "environment"
                productFlavors {
                    ${flavors.joinToString("\n") { "$it {}" }}
                }
            """.trimIndent() else ""
            return """
                plugins {
                    id 'com.android.${if (isAppProject) "application" else "library"}'
                    id 'io.github.reactivecircus.app-versioning'
                }

                ${pluginExtension ?: ""}

                android {
                    compileSdkVersion 30
                    defaultConfig {
                        minSdkVersion 21
                        targetSdkVersion 30
                    }

                    lintOptions {
                        checkReleaseBuilds false
                    }

                    $flavorConfigs
                }
            """.trimIndent()
        }

    val manifestFileContent: String
        get() = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest package="$DEFAULT_PACKAGE_NAME.$projectName" />
        """.trimIndent()

    companion object {
        private const val DEFAULT_PACKAGE_NAME = "io.github.reactivecircus.appversioning"
    }
}

class AppProjectTemplate(
    override val projectName: String = "app",
    override val pluginExtension: String? = null,
    override val useKts: Boolean = true,
    override val flavors: List<String> = emptyList()
) : AndroidProjectTemplate() {
    override val isAppProject: Boolean = true
}

class LibraryProjectTemplate(
    override val projectName: String = "library",
    override val pluginExtension: String? = null,
    override val useKts: Boolean = true,
    override val flavors: List<String> = emptyList()
) : AndroidProjectTemplate() {
    override val isAppProject: Boolean = false
}

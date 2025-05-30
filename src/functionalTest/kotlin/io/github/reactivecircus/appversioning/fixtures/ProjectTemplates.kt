package io.github.reactivecircus.appversioning.fixtures

fun settingsFileContent(
    localBuildCacheUri: String,
    subprojects: List<AndroidProjectTemplate>
) = """
    dependencyResolutionManagement {
        repositories {
            mavenCentral()
            google()
        }
    }

    pluginManagement {
        repositories {
            mavenCentral()
            google()
        }
    }

    buildCache {
        local {
            directory = "$localBuildCacheUri"
        }
    }

    rootProject.name = "app-versioning-fixture"

    ${subprojects.map { it.projectName }.joinToString("\n") { "include(\":$it\")" }}
""".trimIndent()

fun gradlePropertiesFileContent(enableConfigurationCache: Boolean): String {
    val configurationCacheProperties = if (enableConfigurationCache) {
        """
            org.gradle.configuration-cache=true
            org.gradle.unsafe.isolated-projects=true
        """.trimIndent()
    } else {
        ""
    }
    return """
        $configurationCacheProperties
    """.trimIndent()
}

sealed class AndroidProjectTemplate {
    abstract val projectName: String
    abstract val pluginExtension: String?
    abstract val useKts: Boolean
    abstract val flavors: List<String>

    val buildFileContent: String get() = if (useKts) ktsBuildFileContent else groovyBuildFileContent

    private val isAppProject = this is AppProjectTemplate

    private val ktsBuildFileContent: String
        get() {
            val flavorConfigs = if (flavors.isNotEmpty()) {
                """
                flavorDimensions("environment")
                productFlavors {
                    ${flavors.joinToString("\n") { "register(\"$it\") {}" }}
                }
                """.trimIndent()
            } else {
                ""
            }
            val abiConfigs = if (this is AppProjectTemplate) {
                if (splitsApks) {
                    """
                    splits {
                        abi {
                            isEnable = true
                            reset()
                            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                            isUniversalApk = $universalApk
                        }
                    }
                    """.trimIndent()
                } else {
                    ""
                }
            } else {
                ""
            }
            return """
                plugins {
                    id("com.android.${if (isAppProject) "application" else "library"}")
                    id("io.github.reactivecircus.app-versioning")
                }

                ${pluginExtension ?: ""}

                android {
                    namespace = "$DEFAULT_PACKAGE_NAME.${projectName.replace("-", ".")}"
                    compileSdkVersion(34)
                    buildToolsVersion = "34.0.0"
                    defaultConfig {
                        minSdkVersion(21)
                        targetSdkVersion(34)
                    }

                    lintOptions.isCheckReleaseBuilds = false

                    $flavorConfigs

                    $abiConfigs
                }
            """.trimIndent()
        }

    private val groovyBuildFileContent: String
        get() {
            val flavorConfigs = if (flavors.isNotEmpty()) {
                """
                flavorDimensions "environment"
                productFlavors {
                    ${flavors.joinToString("\n") { "$it {}" }}
                }
                """.trimIndent()
            } else {
                ""
            }
            val abiConfigs = if (this is AppProjectTemplate) {
                if (splitsApks) {
                    """
                    splits {
                        abi {
                            enable true
                            reset()
                            include "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
                            universalApk $universalApk
                        }
                    }
                    """.trimIndent()
                } else {
                    ""
                }
            } else {
                ""
            }
            return """
                plugins {
                    id 'com.android.${if (isAppProject) "application" else "library"}'
                    id 'io.github.reactivecircus.app-versioning'
                }

                ${pluginExtension ?: ""}

                android {
                    namespace '$DEFAULT_PACKAGE_NAME.${projectName.replace("-", ".")}'
                    compileSdkVersion 34
                    buildToolsVersion "34.0.0"
                    defaultConfig {
                        minSdkVersion 21
                        targetSdkVersion 34
                    }

                    lintOptions {
                        checkReleaseBuilds false
                    }

                    $flavorConfigs

                    $abiConfigs
                }
            """.trimIndent()
        }

    val manifestFileContent: String
        get() = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest />
        """.trimIndent()

    companion object {
        private const val DEFAULT_PACKAGE_NAME = "io.github.reactivecircus.appversioning"
    }
}

class AppProjectTemplate(
    override val projectName: String = "app",
    override val pluginExtension: String? = null,
    override val useKts: Boolean = true,
    override val flavors: List<String> = emptyList(),
    val splitsApks: Boolean = false,
    val universalApk: Boolean = false,
) : AndroidProjectTemplate()

class LibraryProjectTemplate(
    override val projectName: String = "library",
    override val pluginExtension: String? = null,
    override val useKts: Boolean = true,
    override val flavors: List<String> = emptyList()
) : AndroidProjectTemplate()

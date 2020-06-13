package io.github.reactivecircus.appversioning

import android.databinding.tool.ext.capitalizeUS
import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.android.build.api.variant.VariantOutputConfiguration
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.language.nativeplatform.internal.BuildType
import org.gradle.util.VersionNumber

/**
 * A plugin that generates and sets the version code and version name for an Android app using the latest git tag.
 */
@Suppress("UnstableApiUsage")
class AppVersioningPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val agpVersion = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION)
        require(agpVersion >= VersionNumber.parse(MIN_AGP_VERSION)) {
            "Android App Versioning Gradle Plugin requires Android Gradle Plugin $MIN_AGP_VERSION or later. Detected AGP version is $agpVersion."
        }

        val appVersioningExtension = project.extensions.create("appVersioning", AppVersioningExtension::class.java)

        project.plugins.withType<AppPlugin> {
            project.extensions.getByType<BaseAppModuleExtension>().onVariantProperties {
                if (!appVersioningExtension.releaseBuildOnly.get() || buildType == BuildType.RELEASE.name) {
                    val generateAppVersionInfo = project.registerGenerateAppVersionInfoTask(
                        variantName = name,
                        extension = appVersioningExtension
                    )

                    val generatedVersionCode = generateAppVersionInfo.flatMap { it.versionCode() }
                    val generatedVersionName = generateAppVersionInfo.flatMap { it.versionName() }

                    project.registerPrintAppVersionInfoTask(
                        variantName = name,
                        versionCodeProvider = generatedVersionCode,
                        versionNameProvider = generatedVersionName
                    )

                    val mainOutput = outputs.single { it.outputType == VariantOutputConfiguration.OutputType.SINGLE }
                    mainOutput.versionName.set(generatedVersionName)
                    mainOutput.versionCode.set(generatedVersionCode)
                }
            }
        }
    }

    private fun validateExtensions(extension: AppVersioningExtension) {
        val maxDigits = extension.maxDigits.get()
        require(maxDigits >= AppVersioningExtension.MAX_DIGITS_RANGE_MIN && maxDigits <= AppVersioningExtension.MAX_DIGITS_RANGE_MAX) {
            "`maxDigits` must be at least `${AppVersioningExtension.MAX_DIGITS_RANGE_MIN}` and at most `${AppVersioningExtension.MAX_DIGITS_RANGE_MAX}`."
        }
    }

    private fun Project.registerGenerateAppVersionInfoTask(
        variantName: String,
        extension: AppVersioningExtension
    ): TaskProvider<GenerateAppVersionInfo> = tasks.register(
        "${GenerateAppVersionInfo.TASK_NAME_PREFIX}For${variantName.capitalizeUS()}",
        GenerateAppVersionInfo::class.java
    ) {
        group = APP_VERSIONING_TASK_GROUP
        description = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the $variantName variant."

        gitRefsDirectory.set(rootProject.file(GIT_REFS_DIRECTORY))
        maxDigits.set(extension.maxDigits)
        requireValidTag.set(extension.requireValidGitTag)
        fetchTagsWhenNoneExistsLocally.set(extension.fetchTagsWhenNoneExistsLocally)

        versionCodeFile.set(layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/$variantName/$VERSION_CODE_RESULT_FILE"))
        versionNameFile.set(layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/$variantName/$VERSION_NAME_RESULT_FILE"))
    }

    private fun Project.registerPrintAppVersionInfoTask(
        variantName: String,
        versionCodeProvider: Provider<Int>,
        versionNameProvider: Provider<String>
    ): TaskProvider<PrintAppVersionInfo> = tasks.register(
        "${PrintAppVersionInfo.TASK_NAME_PREFIX}For${variantName.capitalizeUS()}",
        PrintAppVersionInfo::class.java
    ) {
        group = APP_VERSIONING_TASK_GROUP
        description = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the $variantName variant."

        versionCode.set(versionCodeProvider)
        versionName.set(versionNameProvider)
    }

    companion object {
        private const val MIN_AGP_VERSION = "4.0.0"
    }
}

internal const val APP_VERSIONING_TASK_GROUP = "versioning"
internal const val APP_VERSIONING_TASK_OUTPUT_DIR = "outputs/app_versioning"
internal const val GIT_REFS_DIRECTORY = ".git/refs"
internal const val VERSION_CODE_RESULT_FILE = "version_code.txt"
internal const val VERSION_NAME_RESULT_FILE = "version_name.txt"

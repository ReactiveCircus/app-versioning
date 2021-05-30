package io.github.reactivecircus.appversioning

import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.android.build.api.extension.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.VariantOutputConfiguration
import com.android.build.gradle.AppPlugin
import io.github.reactivecircus.appversioning.tasks.GenerateAppVersionInfo
import io.github.reactivecircus.appversioning.tasks.PrintAppVersionInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.language.nativeplatform.internal.BuildType
import org.gradle.util.VersionNumber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A plugin that generates and sets the version code and version name for an Android app using the latest git tag.
 */
@Suppress("UnstableApiUsage")
class AppVersioningPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val agpVersion = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION)
        check(agpVersion >= VersionNumber.parse(MIN_AGP_VERSION)) {
            "Android App Versioning Gradle Plugin requires Android Gradle Plugin $MIN_AGP_VERSION or later. Detected AGP version is $agpVersion."
        }
        val androidAppPluginApplied = AtomicBoolean(false)
        val pluginDisabled = AtomicBoolean(false)
        val appVersioningExtension = project.extensions.create("appVersioning", AppVersioningExtension::class.java)
        project.plugins.withType<AppPlugin> {
            androidAppPluginApplied.set(true)
            project.extensions.getByType<ApplicationAndroidComponentsExtension>().onVariants { variant ->
                if (pluginDisabled.get()) return@onVariants
                if (!appVersioningExtension.enabled.get()) {
                    project.logger.quiet("Android App Versioning plugin is disabled.")
                    pluginDisabled.set(true)
                    return@onVariants
                }
                if (!appVersioningExtension.releaseBuildOnly.get() || variant.buildType == BuildType.RELEASE.name) {
                    val generateAppVersionInfo = project.registerGenerateAppVersionInfoTask(
                        variant = variant,
                        extension = appVersioningExtension
                    )

                    val generatedVersionCode = generateAppVersionInfo.map { it.versionCodeFile.asFile.get().readText().trim().toInt() }
                    val generatedVersionName = generateAppVersionInfo.map { it.versionNameFile.asFile.get().readText().trim() }

                    project.registerPrintAppVersionInfoTask(variantName = variant.name)

                    val mainOutput = variant.outputs.single { it.outputType == VariantOutputConfiguration.OutputType.SINGLE }
                    mainOutput.versionCode.set(generatedVersionCode)
                    mainOutput.versionName.set(generatedVersionName)
                }
            }
        }

        project.afterEvaluate {
            check(androidAppPluginApplied.get()) {
                "Android App Versioning plugin should only be applied to an Android Application project but ${project.displayName} doesn't have the 'com.android.application' plugin applied."
            }
        }
    }

    private fun Project.registerGenerateAppVersionInfoTask(
        variant: ApplicationVariant,
        extension: AppVersioningExtension
    ): TaskProvider<GenerateAppVersionInfo> = tasks.register(
        "${GenerateAppVersionInfo.TASK_NAME_PREFIX}For${variant.name.capitalize()}",
        GenerateAppVersionInfo::class.java
    ) {
        group = APP_VERSIONING_TASK_GROUP
        description = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the ${variant.name} variant."

        gitRefsDirectory.set(project.rootProject.file(GIT_REFS_DIRECTORY).let { rootGradleProject ->
            if (rootGradleProject.exists()) rootGradleProject else {
                extension.gitRootDirectory.takeIf { it.isPresent }?.let { gitRootDirectory ->
                    gitRootDirectory.asFile.orNull?.resolve(GIT_REFS_DIRECTORY)?.takeIf { it.exists() }
                }
            }
        })
        rootProjectDirectory.set(project.rootProject.rootDir)
        rootProjectDisplayName.set(project.rootProject.displayName)
        fetchTagsWhenNoneExistsLocally.set(extension.fetchTagsWhenNoneExistsLocally)
        tagFilter.set(extension.tagFilter)
        kotlinVersionCodeCustomizer.set(extension.kotlinVersionCodeCustomizer)
        kotlinVersionNameCustomizer.set(extension.kotlinVersionNameCustomizer)
        groovyVersionCodeCustomizer.set(extension.groovyVersionCodeCustomizer)
        groovyVersionNameCustomizer.set(extension.groovyVersionNameCustomizer)
        versionCodeFile.set(layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/${variant.name}/$VERSION_CODE_RESULT_FILE"))
        versionNameFile.set(layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/${variant.name}/$VERSION_NAME_RESULT_FILE"))
        variantInfo.set(
            VariantInfo(
                buildType = variant.buildType,
                flavorName = variant.flavorName.orEmpty(),
                variantName = variant.name
            )
        )
    }

    private fun Project.registerPrintAppVersionInfoTask(
        variantName: String
    ): TaskProvider<PrintAppVersionInfo> = tasks.register(
        "${PrintAppVersionInfo.TASK_NAME_PREFIX}For${variantName.capitalize()}",
        PrintAppVersionInfo::class.java
    ) {
        group = APP_VERSIONING_TASK_GROUP
        description = "${PrintAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the $variantName variant."

        versionCodeFile.set(
            layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/$variantName/$VERSION_CODE_RESULT_FILE")
                .flatMap { provider { if (it.asFile.exists()) it else null } }
        )
        versionNameFile.set(
            layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/$variantName/$VERSION_NAME_RESULT_FILE")
                .flatMap { provider { if (it.asFile.exists()) it else null } }
        )
        projectName.set(project.name)
        buildVariantName.set(variantName)
    }

    companion object {
        const val MIN_AGP_VERSION = "4.2.1"
    }
}

internal const val APP_VERSIONING_TASK_GROUP = "versioning"
internal const val APP_VERSIONING_TASK_OUTPUT_DIR = "outputs/app_versioning"
internal const val GIT_REFS_DIRECTORY = ".git/refs"
internal const val VERSION_CODE_RESULT_FILE = "version_code.txt"
internal const val VERSION_NAME_RESULT_FILE = "version_name.txt"

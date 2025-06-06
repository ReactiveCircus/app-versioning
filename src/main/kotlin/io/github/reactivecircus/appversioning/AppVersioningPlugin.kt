package io.github.reactivecircus.appversioning

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.AppPlugin
import io.github.reactivecircus.appversioning.tasks.GenerateAppVersionInfo
import io.github.reactivecircus.appversioning.tasks.PrintAppVersionInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.language.nativeplatform.internal.BuildType
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A plugin that generates and sets the version code and version name for an Android app using the latest git tag.
 */
class AppVersioningPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidAppPluginApplied = AtomicBoolean(false)
        val pluginDisabled = AtomicBoolean(false)
        val appVersioningExtension = project.extensions.create("appVersioning", AppVersioningExtension::class.java)
        project.plugins.withType<AppPlugin> {
            androidAppPluginApplied.set(true)
            val extension = project.extensions.getByType<ApplicationAndroidComponentsExtension>()
            extension.onVariants(selector = extension.selector().all()) { variant ->
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

                    val generatedVersionCode = generateAppVersionInfo.flatMap {
                        it.versionCodeFile.map { file -> file.asFile.readText().trim().toInt() }
                    }
                    val generatedVersionName = generateAppVersionInfo.flatMap {
                        it.versionNameFile.map { file -> file.asFile.readText().trim() }
                    }

                    project.registerPrintAppVersionInfoTask(variantName = variant.name)

                    variant.outputs.forEach { output ->
                        output.versionCode.set(generatedVersionCode)
                        output.versionName.set(generatedVersionName)
                    }
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
        extension: AppVersioningExtension,
    ): TaskProvider<GenerateAppVersionInfo> = tasks.register(
        "${GenerateAppVersionInfo.TASK_NAME_PREFIX}For${variant.name.capitalize()}",
        GenerateAppVersionInfo::class.java
    ) {
        group = APP_VERSIONING_TASK_GROUP
        description = "${GenerateAppVersionInfo.TASK_DESCRIPTION_PREFIX} for the ${variant.name} variant."
        gitRefsDirectory.set(findGitRefsDirectory(extension))
        gitHead.set(findGitHeadFile(extension))
        rootProjectDirectory.set(project.rootProject.rootDir)
        rootProjectDisplayName.set(project.rootProject.displayName)
        fetchTagsWhenNoneExistsLocally.set(extension.fetchTagsWhenNoneExistsLocally)
        tagFilter.set(extension.tagFilter)
        kotlinVersionCodeCustomizer.set(extension.kotlinVersionCodeCustomizer)
        kotlinVersionNameCustomizer.set(extension.kotlinVersionNameCustomizer)
        groovyVersionCodeCustomizer.set(extension.groovyVersionCodeCustomizer)
        groovyVersionNameCustomizer.set(extension.groovyVersionNameCustomizer)
        versionCodeFile.set(
            layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/${variant.name}/$VERSION_CODE_RESULT_FILE")
        )
        versionNameFile.set(
            layout.buildDirectory.file("$APP_VERSIONING_TASK_OUTPUT_DIR/${variant.name}/$VERSION_NAME_RESULT_FILE")
        )
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

    private fun Project.findGitRefsDirectory(extension: AppVersioningExtension): File? {
        return when {
            extension.bareGitRepoDirectory.isPresent -> extension.bareGitRepoDirectory.let { bareGitRepoDirectory ->
                bareGitRepoDirectory.asFile.orNull?.resolve(REFS_DIRECTORY)?.takeIf { it.exists() }
            }
            extension.gitRootDirectory.isPresent -> extension.gitRootDirectory.let { gitRootDirectory ->
                gitRootDirectory.asFile.orNull?.resolve(STANDARD_GIT_REFS_DIRECTORY)?.takeIf { it.exists() }
            }
            else -> project.rootProject.file(STANDARD_GIT_REFS_DIRECTORY).takeIf { it.exists() }
        }
    }

    private fun Project.findGitHeadFile(extension: AppVersioningExtension): File? {
        return when {
            extension.bareGitRepoDirectory.isPresent -> extension.bareGitRepoDirectory.let { bareGitRepoDirectory ->
                bareGitRepoDirectory.asFile.orNull?.resolve(HEAD_FILE)?.takeIf { it.exists() }
            }
            extension.gitRootDirectory.isPresent -> extension.gitRootDirectory.let { gitRootDirectory ->
                gitRootDirectory.asFile.orNull?.resolve(STANDARD_GIT_HEAD_FILE)?.takeIf { it.exists() }
            }
            else -> project.rootProject.file(STANDARD_GIT_HEAD_FILE).takeIf { it.exists() }
        }
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    companion object
}

private const val APP_VERSIONING_TASK_GROUP = "versioning"
private const val APP_VERSIONING_TASK_OUTPUT_DIR = "outputs/app_versioning"
private const val REFS_DIRECTORY = "refs"
private const val HEAD_FILE = "HEAD"
private const val STANDARD_GIT_REFS_DIRECTORY = ".git/$REFS_DIRECTORY"
private const val STANDARD_GIT_HEAD_FILE = ".git/$HEAD_FILE"
private const val VERSION_CODE_RESULT_FILE = "version_code.txt"
private const val VERSION_NAME_RESULT_FILE = "version_name.txt"

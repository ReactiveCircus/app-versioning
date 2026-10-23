package io.github.reactivecircus.appversioning.tasks

import io.github.reactivecircus.appversioning.DefaultVersionContext
import io.github.reactivecircus.appversioning.GitTag
import io.github.reactivecircus.appversioning.VariantInfo
import io.github.reactivecircus.appversioning.VersionCodeSpec
import io.github.reactivecircus.appversioning.VersionNameSpec
import io.github.reactivecircus.appversioning.internal.GitClient
import io.github.reactivecircus.appversioning.toGitTag
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.NormalizeLineEndings

/**
 * Generates app's versionCode and versionName based on git tags.
 */
@CacheableTask
abstract class GenerateAppVersionInfo : DefaultTask() {
    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    abstract val gitRefsDirectory: DirectoryProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    abstract val gitHead: RegularFileProperty

    @get:Internal
    abstract val rootProjectDirectory: DirectoryProperty

    @get:Internal
    abstract val rootProjectDisplayName: Property<String>

    @get:Input
    abstract val fetchTagsWhenNoneExistsLocally: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val tagFilter: Property<String>

    @get:Input
    abstract val versionCodeSpec: Property<VersionCodeSpec>

    @get:Input
    abstract val versionNameSpec: Property<VersionNameSpec>

    @get:OutputFile
    abstract val versionCodeFile: RegularFileProperty

    @get:OutputFile
    abstract val versionNameFile: RegularFileProperty

    @get:Input
    abstract val variantInfo: Property<VariantInfo>

    @TaskAction
    fun generate() {
        check(gitRefsDirectory.isPresent && gitHead.isPresent) {
            "Android App Versioning Gradle Plugin works with git tags but ${rootProjectDisplayName.get()} is not a git root directory, and a valid gitRootDirectory is not provided."
        }

        val gitClient = GitClient.open(rootProjectDirectory.get().asFile)

        val gitTag: GitTag = gitClient.describeLatestTag(
            tagFilter.orNull
        )?.toGitTag() ?: if (fetchTagsWhenNoneExistsLocally.get()) {
            val tagsList = gitClient.listLocalTags()
            if (tagsList.isEmpty()) {
                logger.warn("No git tags found. Fetching tags from remote.")
                gitClient.fetchRemoteTags()
            }
            gitClient.describeLatestTag(tagFilter.orNull)?.toGitTag()
        } else {
            null
        } ?: run {
            logger.warn(
                """
                    No git tags found. Falling back to version code $VERSION_CODE_FALLBACK and version name "$VERSION_NAME_FALLBACK".
                    If you want to fallback to the versionCode and versionName set via the DSL or manifest, or stop generating versionCode and versionName from Git tags:
                    appVersioning {
                        enabled.set(false)
                    }
                """.trimIndent()
            )
            versionCodeFile.get().asFile.writeText(VERSION_CODE_FALLBACK.toString())
            versionNameFile.get().asFile.writeText(VERSION_NAME_FALLBACK)
            return
        }

        val context = DefaultVersionContext(gitTag, variantInfo.get())

        val versionCode = with(versionCodeSpec.get()) { context.versionCode() }
        versionCodeFile.get().asFile.writeText(versionCode.toString())
        logger.quiet("Generated app version code: $versionCode.")

        val versionName = with(versionNameSpec.get()) { context.versionName() }
        versionNameFile.get().asFile.writeText(versionName)
        logger.quiet("Generated app version name: \"$versionName\".")
    }

    internal companion object {
        const val TASK_NAME_PREFIX = "generateAppVersionInfo"
        const val TASK_DESCRIPTION_PREFIX = "Generates app's versionCode and versionName based on git tags"
        const val VERSION_CODE_FALLBACK = 0
        const val VERSION_NAME_FALLBACK = ""
    }
}

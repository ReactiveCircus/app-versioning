@file:Suppress("MagicNumber")

package io.github.reactivecircus.appversioning.tasks

import groovy.lang.Closure
import io.github.reactivecircus.appversioning.GitTag
import io.github.reactivecircus.appversioning.VersionCodeCustomizer
import io.github.reactivecircus.appversioning.VersionNameCustomizer
import io.github.reactivecircus.appversioning.internal.GitClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import kotlin.math.pow

/**
 * Generates app's versionCode and versionName based on git tags.
 */
@CacheableTask
abstract class GenerateAppVersionInfo : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gitRefsDirectory: DirectoryProperty

    @get:Input
    abstract val requireValidTag: Property<Boolean>

    @get:Input
    abstract val fetchTagsWhenNoneExistsLocally: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val kotlinVersionCodeCustomizer: Property<VersionCodeCustomizer>

    @get:Optional
    @get:Input
    abstract val kotlinVersionNameCustomizer: Property<VersionNameCustomizer>

    @get:Optional
    @get:Input
    abstract val groovyVersionCodeCustomizer: Property<Closure<Int>>

    @get:Optional
    @get:Input
    abstract val groovyVersionNameCustomizer: Property<Closure<String>>

    @get:OutputFile
    abstract val versionCodeFile: RegularFileProperty

    @get:OutputFile
    abstract val versionNameFile: RegularFileProperty

    @TaskAction
    fun generate() {
        check(gitRefsDirectory.isPresent) {
            "Android App Versioning Gradle Plugin works with git tags but ${project.rootProject.displayName} is not a valid git repository."
        }

        val gitClient = GitClient.open(project.rootProject.rootDir)

        val gitTag: GitTag =
            gitClient.getLatestGitTag(MAX_DIGITS_ALLOCATED) ?: if (fetchTagsWhenNoneExistsLocally.get()) {
                val tagsList = gitClient.listLocalTags()
                if (tagsList.isEmpty()) {
                    logger.warn("No git tags found. Fetching tags from remote.")
                    gitClient.fetchRemoteTags()
                }
                gitClient.getLatestGitTag(MAX_DIGITS_ALLOCATED)
            } else {
                null
            }.let {
                if (requireValidTag.get()) {
                    requireNotNull(it) {
                        """
                        Could not find a git tag that follows semantic versioning.
                        Note that tags with additional labels after MAJOR.MINOR.PATCH are ignored.
                    """.trimIndent()
                    }
                } else {
                    logger.warn("No valid git tag found. Falling back to version name \"0.0.0\" and version code 0.")
                    GitTag.FALLBACK
                }
            }

        val versionCode = when {
            kotlinVersionCodeCustomizer.isPresent -> kotlinVersionCodeCustomizer.get().invoke(gitTag, project.providers)
            groovyVersionCodeCustomizer.isPresent -> groovyVersionCodeCustomizer.get().call(gitTag, project.providers)
            else -> gitTag.major * 10.0.pow(MAX_DIGITS_ALLOCATED * 2).toInt() +
                    gitTag.minor * 10.0.pow(MAX_DIGITS_ALLOCATED).toInt() +
                    gitTag.patch + gitTag.commitsSinceLatestTag // TODO do not add build number by default. Can be achieved with `overrideVersionCode`.
        }
        versionCodeFile.get().asFile.writeText(versionCode.toString())
        logger.quiet("Generated app version code: $versionCode.")

        val versionName = when {
            kotlinVersionNameCustomizer.isPresent -> kotlinVersionNameCustomizer.get().invoke(gitTag, project.providers)
            groovyVersionNameCustomizer.isPresent -> groovyVersionNameCustomizer.get().call(gitTag, project.providers)
            else -> gitTag.toString()
        }
        versionNameFile.get().asFile.writeText(versionName)
        logger.quiet("Generated app version name: \"$versionName\".")
    }

    /**
     * Returns the generated app version code as a `Provider<Int>`.
     */
    fun versionCode(): Provider<Int> = versionCodeFile.asFile.map { file ->
        file.readText().trim().toInt()
    }

    /**
     * Returns the generated app version name as a `Provider<String>`.
     */
    fun versionName(): Provider<String> = versionNameFile.asFile.map { file ->
        file.readText().trim()
    }

    companion object {
        const val TASK_NAME_PREFIX = "generateAppVersionInfo"
        const val TASK_DESCRIPTION_PREFIX = "Generates app's versionCode and versionName based on git tags"
        private const val MAX_DIGITS_ALLOCATED = 2
    }
}

private fun GitClient.getLatestGitTag(maxDigits: Int): GitTag? =
    describeLatestTag("[0-9]*.[0-9]*.[0-9]*")?.let {
        it.replace("-\\bg[0-9a-f]{5,40}\\b".toRegex(), "")
            .replace("[a-zA-Z]".toRegex(), "")
            .replace("-", ".")
            .let { tag ->
                if (buildGitTagRegex(maxDigits).matches(tag)) tag else ""
            }
            .split(".")
            .let { parts ->
                if (parts.size == 4) {
                    GitTag(
                        major = parts[0].toInt(),
                        minor = parts[1].toInt(),
                        patch = parts[2].toInt(),
                        commitsSinceLatestTag = parts[3].toInt()
                    )
                } else null
            }
    }

// TODO do not filter based on [maxDigits], check if matched tag has version part that exceeds 2 digits and crash and suggest using `overrideVersionCode`.
private fun buildGitTagRegex(maxDigits: Int): Regex =
    "^(0|[1-9]\\d{0,${maxDigits - 1}})\\.(0|[1-9]\\d{0,${maxDigits - 1}})\\.(0|[1-9]\\d{0,${maxDigits - 1}})\\.(0|[1-9]\\d*)\$".toRegex()

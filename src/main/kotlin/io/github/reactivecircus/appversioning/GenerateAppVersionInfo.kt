@file:Suppress("MagicNumber")

package io.github.reactivecircus.appversioning

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import kotlin.math.pow

/**
 * Generates app's versionCode and versionName based on git-tag.
 */
@CacheableTask
abstract class GenerateAppVersionInfo : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gitRefsDirectory: DirectoryProperty

    @get:Input
    abstract val maxDigits: Property<Int>

    @get:Input
    abstract val requireValidTag: Property<Boolean>

    @get:Input
    abstract val fetchTagsWhenNoneExistsLocally: Property<Boolean>

    @get:Input
    abstract val versionCodeCustomizer: Property<VersionCodeCustomizer>

    @get:Input
    abstract val versionNameCustomizer: Property<VersionNameCustomizer>

    @get:OutputFile
    abstract val versionCodeFile: RegularFileProperty

    @get:OutputFile
    abstract val versionNameFile: RegularFileProperty

    @TaskAction
    fun generate() {
        check(project.rootProject.file(".git").exists()) {
            "${project.rootProject.displayName} is not a git repository."
        }

        val maxDigitsAllowed = maxDigits.get()
        check(maxDigitsAllowed in MAX_DIGITS_RANGE_MIN..MAX_DIGITS_RANGE_MAX) {
            "`maxDigits` must be at least `$MAX_DIGITS_RANGE_MIN` and at most `$MAX_DIGITS_RANGE_MAX`."
        }

        val gitTagInfo: GitTagInfo =
            project.getGitTagInfo(maxDigitsAllowed) ?: if (fetchTagsWhenNoneExistsLocally.get()) {
                project.fetchGitTagsIfNoneExistsLocally()
                project.getGitTagInfo(maxDigitsAllowed)
            } else {
                null
            }.let {
                if (requireValidTag.get()) {
                    requireNotNull(it) {
                        """
                        Could not find a git tag that follows semantic versioning.
                        Note that tags with additional labels after MAJOR.MINOR.PATCH are ignored.
                        Tags with more than $maxDigitsAllowed digits for any of the MAJOR, MINOR or PATCH version are also ignored.
                    """.trimIndent()
                    }
                } else {
                    logger.warn("No valid git tag found. Falling back to version name \"0.0.0\" and version code 0.")
                    GitTagInfo.FALLBACK
                }
            }

        val versionCode = versionCodeCustomizer.get().invoke(gitTagInfo).takeIf {
            it > Int.MIN_VALUE
        } ?: gitTagInfo.major * 10.0.pow(maxDigitsAllowed * 2).toInt() +
        gitTagInfo.minor * 10.0.pow(maxDigitsAllowed).toInt() +
        gitTagInfo.patch +
        gitTagInfo.commitsSinceLatestTag
        versionCodeFile.get().asFile.writeText(versionCode.toString())
        logger.lifecycle("Generated app version code: $versionCode.")

        val versionName = versionNameCustomizer.get().invoke(gitTagInfo).ifBlank { gitTagInfo.toString() }
        versionNameFile.get().asFile.writeText(versionName)
        logger.lifecycle("Generated app version name: \"$versionName\".")
    }

    /**
     * Returns the generated app version name as a `Provider<String>`.
     */
    @Suppress("UnstableApiUsage")
    fun versionName(): Provider<String> = versionNameFile.asFile.map { file ->
        file.readText().trim()
    }

    /**
     * Returns the generated app version code as a `Provider<Int>`.
     */
    @Suppress("UnstableApiUsage")
    fun versionCode(): Provider<Int> = versionCodeFile.asFile.map { file ->
        file.readText().trim().toInt()
    }

    companion object {
        const val TASK_NAME_PREFIX = "generateAppVersionInfo"
        const val TASK_DESCRIPTION_PREFIX = "Generates app's versionCode and versionName based on git-tag"
        internal const val MAX_DIGITS_RANGE_MIN = 1
        internal const val MAX_DIGITS_RANGE_MAX = 4
    }
}

class GitTagInfo(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val commitsSinceLatestTag: Int
) {
    override fun toString(): String {
        return "$major.$minor.$patch".let { semVersion ->
            if (commitsSinceLatestTag > 0) {
                "$semVersion.$commitsSinceLatestTag"
            } else {
                semVersion
            }
        }
    }

    companion object {
        val FALLBACK = GitTagInfo(0, 0, 0, 0)
    }
}

// TODO add commitHash and commitsInCurrentBranch to GitTagInfo
private fun Project.getGitTagInfo(maxDigits: Int): GitTagInfo? =
    "git describe --match [0-9]*.[0-9]*.[0-9]* --tags --long"
        .trimIndent().execute(workingDir = rootDir)
        .replace("-\\bg[0-9a-f]{5,40}\\b".toRegex(), "")
        .replace("[a-zA-Z]".toRegex(), "")
        .replace("-", ".")
        .let { tag ->
            if (buildGitTagRegex(maxDigits).matches(tag)) tag else ""
        }
        .split(".")
        .let { parts ->
            if (parts.size == 4) {
                GitTagInfo(
                    major = parts[0].toInt(),
                    minor = parts[1].toInt(),
                    patch = parts[2].toInt(),
                    commitsSinceLatestTag = parts[3].toInt()
                )
            } else null
        }

private fun buildGitTagRegex(maxDigits: Int): Regex =
    "^(0|[1-9]\\d{0,${maxDigits - 1}})\\.(0|[1-9]\\d{0,${maxDigits - 1}})\\.(0|[1-9]\\d{0,${maxDigits - 1}})\\.(0|[1-9]\\d*)\$".toRegex()

private fun Project.fetchGitTagsIfNoneExistsLocally() {
    val tagsList = "git tag --list".execute(workingDir = rootDir)
    if (tagsList.isEmpty()) {
        logger.warn("No git tags found. Fetching tags from remote.")
        "git fetch --tag".execute(workingDir = rootDir)
    }
}

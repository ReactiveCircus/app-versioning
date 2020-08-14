package io.github.reactivecircus.appversioning.tasks

import groovy.lang.Closure
import io.github.reactivecircus.appversioning.GitTag
import io.github.reactivecircus.appversioning.VersionCodeCustomizer
import io.github.reactivecircus.appversioning.VersionNameCustomizer
import io.github.reactivecircus.appversioning.internal.GitClient
import io.github.reactivecircus.appversioning.toGitTag
import io.github.reactivecircus.appversioning.toInt
import io.github.reactivecircus.appversioning.toSemVer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Generates app's versionCode and versionName based on git tags.
 */
@CacheableTask
abstract class GenerateAppVersionInfo @Inject constructor(private val providers: ProviderFactory) : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gitRefsDirectory: DirectoryProperty

    @get:Internal
    abstract val rootProjectDirectory: DirectoryProperty

    @get:Internal
    abstract val rootProjectDisplayName: Property<String>

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
            "Android App Versioning Gradle Plugin works with git tags but ${rootProjectDisplayName.get()} is not a valid git repository."
        }

        val gitClient = GitClient.open(rootProjectDirectory.get().asFile)

        val gitTag: GitTag = gitClient.describeLatestTag()?.toGitTag() ?: if (fetchTagsWhenNoneExistsLocally.get()) {
            val tagsList = gitClient.listLocalTags()
            if (tagsList.isEmpty()) {
                logger.warn("No git tags found. Fetching tags from remote.")
                gitClient.fetchRemoteTags()
            }
            gitClient.describeLatestTag()?.toGitTag()
        } else {
            null
        } ?: run {
            // TODO improve fallback mechanism
            logger.warn("No git tags found. Falling back to version code 0 and version name \"0\".")
            versionCodeFile.get().asFile.writeText(0.toString())
            logger.quiet("Generated app version code: 0.")
            versionNameFile.get().asFile.writeText("0")
            logger.quiet("Generated app version name: \"0\".")
            return
        }

        val versionCode: Int = generateVersionCodeFromGitTag(gitTag)
        versionCodeFile.get().asFile.writeText(versionCode.toString())
        logger.quiet("Generated app version code: $versionCode.")

        val versionName: String = when {
            kotlinVersionNameCustomizer.isPresent -> kotlinVersionNameCustomizer.get().invoke(gitTag, providers)
            groovyVersionNameCustomizer.isPresent -> groovyVersionNameCustomizer.get().call(gitTag, providers)
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

    private fun generateVersionCodeFromGitTag(gitTag: GitTag): Int = when {
        kotlinVersionCodeCustomizer.isPresent -> kotlinVersionCodeCustomizer.get().invoke(gitTag, providers)
        groovyVersionCodeCustomizer.isPresent -> groovyVersionCodeCustomizer.get().call(gitTag, providers)
        else -> {
            // no custom rule for generating versionCode has been provided, attempt calculation based on SemVer
            val semVer = runCatching {
                gitTag.toSemVer()
            }.getOrNull()
            checkNotNull(semVer) {
                """
                    Could not generate versionCode as "${gitTag.rawTagName}" does not follow semantic versioning.
                    Please either ensure latest git tag follows semantic versioning, or provide a custom rule for generating versionCode using the `overrideVersionCode` lambda.
                """.trimIndent()
            }
            runCatching {
                semVer.toInt(maxDigitsPerComponent = MAX_DIGITS_PER_SEM_VER_COMPONENT)
            }.getOrElse {
                Int.MAX_VALUE
                throw IllegalStateException(
                    """
                        Could not generate versionCode from "${gitTag.rawTagName}" as the SemVer cannot be represented as an Integer.
                        This is usually because MAJOR or MINOR version is greater than 99, as by default maximum of 2 digits is allowed for MINOR and PATCH components of a SemVer tag.
                        Another reason might be that the overall positional notation of the SemVer (MAJOR * 10000 + MINOR * 100 + PATCH) is greater than the maximum value of an integer (2147483647).
                        As a workaround you can provide a custom rule for generating versionCode using the `overrideVersionCode` lambda.
                    """.trimIndent()
                )
            }
        }
    }

    companion object {
        const val TASK_NAME_PREFIX = "generateAppVersionInfo"
        const val TASK_DESCRIPTION_PREFIX = "Generates app's versionCode and versionName based on git tags"
        private const val MAX_DIGITS_PER_SEM_VER_COMPONENT = 2
    }
}

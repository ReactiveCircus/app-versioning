@file:Suppress("UnstableApiUsage", "DuplicatedCode")

package io.github.reactivecircus.appversioning.tasks

import groovy.lang.Closure
import io.github.reactivecircus.appversioning.GitTag
import io.github.reactivecircus.appversioning.VariantInfo
import io.github.reactivecircus.appversioning.VersionCodeCustomizer
import io.github.reactivecircus.appversioning.VersionNameCustomizer
import io.github.reactivecircus.appversioning.internal.GitClient
import io.github.reactivecircus.appversioning.toGitTag
import io.github.reactivecircus.appversioning.toInt
import io.github.reactivecircus.appversioning.toSemVer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
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
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Generates app's versionCode and versionName based on git tags.
 */
@CacheableTask
abstract class GenerateAppVersionInfo @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

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

    @get:Input
    abstract val variantInfo: Property<VariantInfo>

    @TaskAction
    fun generate() {
        workerExecutor.noIsolation().submit(GenerateAppVersionInfoWorkAction::class.java) {
            gitRefsDirectory.set(this@GenerateAppVersionInfo.gitRefsDirectory)
            gitHead.set(this@GenerateAppVersionInfo.gitHead)
            rootProjectDirectory.set(this@GenerateAppVersionInfo.rootProjectDirectory)
            rootProjectDisplayName.set(this@GenerateAppVersionInfo.rootProjectDisplayName)
            fetchTagsWhenNoneExistsLocally.set(this@GenerateAppVersionInfo.fetchTagsWhenNoneExistsLocally)
            tagFilter.set(this@GenerateAppVersionInfo.tagFilter)
            kotlinVersionCodeCustomizer.set(this@GenerateAppVersionInfo.kotlinVersionCodeCustomizer)
            kotlinVersionNameCustomizer.set(this@GenerateAppVersionInfo.kotlinVersionNameCustomizer)
            groovyVersionCodeCustomizer.set(this@GenerateAppVersionInfo.groovyVersionCodeCustomizer)
            groovyVersionNameCustomizer.set(this@GenerateAppVersionInfo.groovyVersionNameCustomizer)
            versionCodeFile.set(this@GenerateAppVersionInfo.versionCodeFile)
            versionNameFile.set(this@GenerateAppVersionInfo.versionNameFile)
            variantInfo.set(this@GenerateAppVersionInfo.variantInfo)
        }
    }

    companion object {
        const val TASK_NAME_PREFIX = "generateAppVersionInfo"
        const val TASK_DESCRIPTION_PREFIX = "Generates app's versionCode and versionName based on git tags"
        const val VERSION_CODE_FALLBACK = 0
        const val VERSION_NAME_FALLBACK = ""
    }
}

private interface GenerateAppVersionInfoWorkParameters : WorkParameters {
    val gitRefsDirectory: DirectoryProperty
    val gitHead: RegularFileProperty
    val rootProjectDirectory: DirectoryProperty
    val rootProjectDisplayName: Property<String>
    val fetchTagsWhenNoneExistsLocally: Property<Boolean>
    val tagFilter: Property<String>
    val kotlinVersionCodeCustomizer: Property<VersionCodeCustomizer>
    val kotlinVersionNameCustomizer: Property<VersionNameCustomizer>
    val groovyVersionCodeCustomizer: Property<Closure<Int>>
    val groovyVersionNameCustomizer: Property<Closure<String>>
    val versionCodeFile: RegularFileProperty
    val versionNameFile: RegularFileProperty
    val variantInfo: Property<VariantInfo>
}

private abstract class GenerateAppVersionInfoWorkAction @Inject constructor(
    private val providers: ProviderFactory
) : WorkAction<GenerateAppVersionInfoWorkParameters> {

    private val logger = Logging.getLogger(GenerateAppVersionInfo::class.java)

    @Suppress("LongMethod")
    override fun execute() {
        val gitRefsDirectory = parameters.gitRefsDirectory
        val gitHead = parameters.gitHead
        val rootProjectDirectory = parameters.rootProjectDirectory
        val rootProjectDisplayName = parameters.rootProjectDisplayName
        val fetchTagsWhenNoneExistsLocally = parameters.fetchTagsWhenNoneExistsLocally
        val tagFilter = parameters.tagFilter
        val kotlinVersionCodeCustomizer = parameters.kotlinVersionCodeCustomizer
        val kotlinVersionNameCustomizer = parameters.kotlinVersionNameCustomizer
        val groovyVersionCodeCustomizer = parameters.groovyVersionCodeCustomizer
        val groovyVersionNameCustomizer = parameters.groovyVersionNameCustomizer
        val versionCodeFile = parameters.versionCodeFile
        val versionNameFile = parameters.versionNameFile
        val variantInfo = parameters.variantInfo

        check(gitRefsDirectory.isPresent && gitHead.isPresent) {
            "Android App Versioning Gradle Plugin works with git tags but ${rootProjectDisplayName.get()} is not a git root directory, and a valid gitRootDirectory is not provided."
        }

        val gitClient = GitClient.open(rootProjectDirectory.get().asFile)

        val gitTag: GitTag = gitClient.describeLatestTag(tagFilter.orNull)?.toGitTag() ?: if (fetchTagsWhenNoneExistsLocally.get()) {
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
                    No git tags found. Falling back to version code ${GenerateAppVersionInfo.VERSION_CODE_FALLBACK} and version name "${GenerateAppVersionInfo.VERSION_NAME_FALLBACK}".
                    If you want to fallback to the versionCode and versionName set via the DSL or manifest, or stop generating versionCode and versionName from Git tags:
                    appVersioning {
                        enabled.set(false)
                    }
                """.trimIndent()
            )
            versionCodeFile.get().asFile.writeText(GenerateAppVersionInfo.VERSION_CODE_FALLBACK.toString())
            versionNameFile.get().asFile.writeText(GenerateAppVersionInfo.VERSION_NAME_FALLBACK)
            return
        }

        val versionCode: Int = generateVersionCodeFromGitTag(
            variantInfo,
            gitTag,
            kotlinVersionCodeCustomizer,
            groovyVersionCodeCustomizer
        )
        versionCodeFile.get().asFile.writeText(versionCode.toString())
        logger.quiet("Generated app version code: $versionCode.")

        val versionName: String = when {
            kotlinVersionNameCustomizer.isPresent -> kotlinVersionNameCustomizer.get().invoke(
                gitTag,
                providers,
                variantInfo.get()
            )
            groovyVersionNameCustomizer.isPresent -> groovyVersionNameCustomizer.get().call(
                gitTag,
                providers,
                variantInfo.get()
            )
            else -> gitTag.toString()
        }
        versionNameFile.get().asFile.writeText(versionName)
        logger.quiet("Generated app version name: \"$versionName\".")
    }

    private fun generateVersionCodeFromGitTag(
        variantInfo: Property<VariantInfo>,
        gitTag: GitTag,
        kotlinVersionCodeCustomizer: Property<VersionCodeCustomizer>,
        groovyVersionCodeCustomizer: Property<Closure<Int>>
    ): Int = when {
        kotlinVersionCodeCustomizer.isPresent -> kotlinVersionCodeCustomizer.get().invoke(
            gitTag,
            providers,
            variantInfo.get()
        )
        groovyVersionCodeCustomizer.isPresent -> groovyVersionCodeCustomizer.get().call(
            gitTag,
            providers,
            variantInfo.get()
        )
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
        private const val MAX_DIGITS_PER_SEM_VER_COMPONENT = 2
    }
}

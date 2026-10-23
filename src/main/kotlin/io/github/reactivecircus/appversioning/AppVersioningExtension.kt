package io.github.reactivecircus.appversioning

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * Extension for [AppVersioningPlugin].
 */
@Suppress("unused")
open class AppVersioningExtension internal constructor(objects: ObjectFactory) {
    /**
     * Whether to enable the plugin.
     *
     * Default is `true`.
     */
    val enabled = objects.property<Boolean>().convention(DEFAULT_ENABLED)

    /**
     * Whether to only generate version name and version code for `release` builds.
     *
     * Default is `false`.
     */
    val releaseBuildOnly = objects.property<Boolean>().convention(DEFAULT_RELEASE_BUILD_ONLY)

    /**
     * Whether to fetch git tags from remote when no git tags can be found locally.
     *
     * Default is `false`.
     */
    val fetchTagsWhenNoneExistsLocally = objects.property<Boolean>().convention(
        DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY
    )

    /**
     * Git root directory used for fetching git tags.
     * Use this to explicitly set the git root directory when the root Gradle project is not the git root directory.
     */
    val gitRootDirectory = objects.directoryProperty().convention(null)

    /**
     * Bare Git repository directory.
     * Use this to explicitly set the directory of a bare git repository (e.g. `app.git`) instead of the standard `.git`.
     * Setting this will override the value of [gitRootDirectory] property.
     */
    val bareGitRepoDirectory = objects.directoryProperty().convention(null)

    /**
     * Custom glob pattern for matching git tags.
     */
    val tagFilter = objects.property<String>().convention(null)

    /**
     * Specification for generating `versionCode`.
     *
     * Default is [VersionCodeSpec.SemVer].
     */
    val versionCode: Property<VersionCodeSpec> = objects.property<VersionCodeSpec>().convention(VersionCodeSpec.SemVer)

    /**
     * Specification for generating `versionName`.
     *
     * Default is [VersionNameSpec.TagName].
     */
    val versionName: Property<VersionNameSpec> =
        objects.property<VersionNameSpec>().convention(VersionNameSpec.TagName)

    /**
     * Configures [versionCode] using a Groovy [Closure] with [VersionContext] as its delegate.
     */
    fun versionCode(
        @DelegatesTo(VersionContext::class, strategy = Closure.DELEGATE_FIRST)
        closure: Closure<Int>,
    ) {
        versionCode.set(closure.toVersionCodeSpec())
    }

    /**
     * Configures [versionName] using a Groovy [Closure] with [VersionContext] as its delegate.
     */
    fun versionName(
        @DelegatesTo(VersionContext::class, strategy = Closure.DELEGATE_FIRST)
        closure: Closure<CharSequence>,
    ) {
        versionName.set(closure.toVersionNameSpec())
    }

    internal companion object {
        const val DEFAULT_ENABLED = true
        const val DEFAULT_RELEASE_BUILD_ONLY = false
        const val DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY = false
    }
}

private inline fun <reified T : Any> ObjectFactory.property(): Property<T> = property(T::class.java)

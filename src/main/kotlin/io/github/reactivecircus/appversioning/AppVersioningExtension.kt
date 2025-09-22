package io.github.reactivecircus.appversioning

import groovy.lang.Closure
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory

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
     * Provides a custom rule for generating versionCode by implementing a [GitTag], [ProviderFactory], [VariantInfo] -> Int lambda.
     * [GitTag] is generated from latest git tag lazily by the plugin during task execution.
     * [ProviderFactory] can be used for fetching environment variables, Gradle and system properties.
     * [VariantInfo] can be used to customize version code based on the build variants (product flavors and build types).
     *
     * By default the plugin attempts to generate the versionCode by converting a SemVer compliant tag to an integer
     * using positional notation: versionCode = MAJOR * 10000 + MINOR * 100 + PATCH
     *
     * If your tags don't follow semantic versioning, you don't like the default formula used to convert a SemVer tag to versionCode,
     * or if you want to fully customize how the versionCode is generated, you can implement this lambda to provide your own versionCode generation rule.
     *
     * Note that you can use the `gitTag.toSemVer()` extension (or `SemVer.fromGitTag(gitTag)` if you use groovy) to get a type-safe `SemVer` model
     * if your custom rule is still based on semantic versioning.
     */
    fun overrideVersionCode(customizer: VersionCodeCustomizer) {
        kotlinVersionCodeCustomizer.set(customizer)
    }

    /**
     * Same as `overrideVersionCode(customizer: VersionCodeCustomizer)` above but for groovy support.
     */
    fun overrideVersionCode(customizer: Closure<Int>) {
        groovyVersionCodeCustomizer.set(customizer.dehydrate())
    }

    /**
     * Provides a custom rule for generating versionName by implementing a [GitTag], [ProviderFactory], [VariantInfo] -> String lambda.
     * [GitTag] is generated from latest git tag lazily by the plugin during task execution.
     * [ProviderFactory] can be used for fetching environment variables, Gradle and system properties.
     * [VariantInfo] can be used to customize version name based on the build variants (product flavors and build types).
     *
     * This is useful if you want to fully customize how the versionName is generated.
     * If not specified, versionName will be the name of the latest git tag.
     */
    fun overrideVersionName(customizer: VersionNameCustomizer) {
        kotlinVersionNameCustomizer.set(customizer)
    }

    /**
     * Same as `overrideVersionName(customizer: VersionNameCustomizer)` above but for groovy support.
     */
    fun overrideVersionName(customizer: Closure<String>) {
        groovyVersionNameCustomizer.set(customizer.dehydrate())
    }

    /**
     * A lambda (Kotlin function type) for specifying a custom rule for generating versionCode.
     */
    internal val kotlinVersionCodeCustomizer = objects.property<VersionCodeCustomizer>()

    /**
     * A lambda (Groovy closure) for specifying a custom rule for generating versionCode.
     */
    internal val groovyVersionCodeCustomizer = objects.property<Closure<Int>>()

    /**
     * A lambda (Kotlin function type) for specifying a custom rule for generating versionName.
     */
    internal val kotlinVersionNameCustomizer = objects.property<VersionNameCustomizer>()

    /**
     * A lambda (Groovy closure) for specifying a custom rule for generating versionName.
     */
    internal val groovyVersionNameCustomizer = objects.property<Closure<String>>()

    companion object {
        internal const val DEFAULT_ENABLED = true
        internal const val DEFAULT_RELEASE_BUILD_ONLY = false
        internal const val DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY = false
    }
}

private inline fun <reified T : Any> ObjectFactory.property(): Property<T> = property(T::class.java)

internal typealias VersionCodeCustomizer = (GitTag, ProviderFactory, VariantInfo) -> Int
internal typealias VersionNameCustomizer = (GitTag, ProviderFactory, VariantInfo) -> String

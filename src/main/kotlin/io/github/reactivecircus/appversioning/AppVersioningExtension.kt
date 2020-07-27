package io.github.reactivecircus.appversioning

import groovy.lang.Closure
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.property

/**
 * Extension for [AppVersioningPlugin].
 */
@Suppress("UnstableApiUsage", "unused")
open class AppVersioningExtension internal constructor(objects: ObjectFactory) {

    /**
     * Whether to only generate version name and version code for `release` builds.
     *
     * Default is `true`.
     */
    val releaseBuildOnly = objects.property<Boolean>().convention(DEFAULT_RELEASE_BUILD_ONLY)

    /**
     * Whether a valid git tag is required.
     * When set to `true` a git tag in the MAJOR.MINOR.PATCH format must be present.
     * When set to `false` version name "0.0.0" and version code 0 will be used if no valid git tag exists.
     *
     * Default is `false`.
     */
    val requireValidGitTag = objects.property<Boolean>().convention(DEFAULT_REQUIRE_VALID_GIT_TAG)

    /**
     * Whether to fetch git tags from remote when no valid git tag can be found locally.
     *
     * Default is `false`.
     */
    val fetchTagsWhenNoneExistsLocally = objects.property<Boolean>().convention(
        DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY
    )

    /**
     * Provides a custom rule for generating versionCode by implementing a [GitTag], [ProviderFactory] -> Int lambda.
     * The [GitTag] is computed lazily by the plugin during task execution, whereas the [ProviderFactory] can be used for fetching
     * environment variables, Gradle and system properties.
     *
     * This is useful if you want to fully customize how the versionCode is generated.
     * If not specified, versionCode will be computed from the latest git tag that follows semantic versioning.
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
     * Provides a custom rule for generating versionName by implementing a [GitTag], [ProviderFactory] -> String lambda.
     * The [GitTag] is computed lazily by the plugin during task execution, whereas the [ProviderFactory] can be used for fetching
     * environment variables, Gradle and system properties.
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
        internal const val DEFAULT_RELEASE_BUILD_ONLY = true
        internal const val DEFAULT_REQUIRE_VALID_GIT_TAG = false
        internal const val DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY = false
    }
}

internal typealias VersionCodeCustomizer = (GitTag, ProviderFactory) -> Int
internal typealias VersionNameCustomizer = (GitTag, ProviderFactory) -> String

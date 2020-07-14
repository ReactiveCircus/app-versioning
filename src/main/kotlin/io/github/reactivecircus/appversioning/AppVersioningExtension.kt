package io.github.reactivecircus.appversioning

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

/**
 * Extension for [AppVersioningPlugin].
 */
@Suppress("UnstableApiUsage")
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
     * Maximum number of digits allowed for any of the MAJOR, MINOR, or PATCH version.
     * E.g. when set to `3`, the maximum version allowed for MAJOR, MINOR, or PATCH is 999.
     *
     * Must be at least `1` and at most `4`.
     *
     * Default is `3`.
     */
    val maxDigits = objects.property<Int>().convention(DEFAULT_MAX_DIGITS)

    /**
     * Provides a custom rule for generating versionCode by implementing a [GitTag] -> Int lambda, where the [GitTag] is computed and provided
     * lazily during task execution. This is useful if you want to fully customize how the versionCode is generated.
     * If not specified, versionCode will be computed from the latest git tag.
     */
    fun overrideVersionCode(customizer: VersionCodeCustomizer) {
        versionCodeCustomizer.set(customizer)
    }

    /**
     * Provides a custom rule for generating versionName by implementing a [GitTag] -> String lambda, where the [GitTag] is computed and provided
     * lazily during task execution. This is useful if you want to fully customize how the versionName is generated.
     * If not specified, versionName will be computed from the latest git tag.
     */
    fun overrideVersionName(customizer: VersionNameCustomizer) {
        versionNameCustomizer.set(customizer)
    }

    /**
     * A lambda for specifying a custom rule for generating versionCode.
     *
     * Default is `Int.MIN_VALUE` which indicates no custom rule has been specified.
     */
    internal val versionCodeCustomizer = objects.property<VersionCodeCustomizer>().convention { Int.MIN_VALUE }

    /**
     * A lambda for specifying a custom rule for generating versionName.
     *
     * Default is `""` (empty string) which indicates no custom rule has been specified.
     */
    internal val versionNameCustomizer = objects.property<VersionNameCustomizer>().convention { "" }

    companion object {
        const val DEFAULT_RELEASE_BUILD_ONLY = true
        const val DEFAULT_REQUIRE_VALID_GIT_TAG = false
        const val DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY = false
        const val DEFAULT_MAX_DIGITS = 3
    }
}

internal typealias VersionCodeCustomizer = (GitTag) -> Int
internal typealias VersionNameCustomizer = (GitTag) -> String

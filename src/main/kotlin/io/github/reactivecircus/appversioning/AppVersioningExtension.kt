package io.github.reactivecircus.appversioning

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

/**
 * Extension for [AppVersioningPlugin].
 */
open class AppVersioningExtension internal constructor(objects: ObjectFactory) {

    /**
     * Whether to only generate version name and version code for `release` builds.
     *
     * Default is `true`.
     */
    val releaseBuildOnly = objects.property<Boolean>().apply {
        set(DEFAULT_RELEASE_BUILD_ONLY)
    }

    /**
     * Whether a valid git tag is required.
     * When set to `true` a git tag in the MAJOR.MINOR.PATCH format must be present.
     * When set to `false` version name "0.0.0" and version code 0 will be used if no valid git tag exists.
     *
     * Default is `false`.
     */
    val requireValidGitTag = objects.property<Boolean>().apply {
        set(DEFAULT_REQUIRE_VALID_GIT_TAG)
    }

    /**
     * Whether to fetch git tags from remote when no valid git tag can be found locally.
     *
     * Default is `false`.
     */
    val fetchTagsWhenNoneExistsLocally = objects.property<Boolean>().apply {
        set(DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY)
    }

    /**
     * Maximum number of digits allowed for any of the MAJOR, MINOR, or PATCH version.
     * E.g. when set to `3`, the maximum version allowed for MAJOR, MINOR, or PATCH is 999.
     *
     * Must be at least `1` and at most `4`.
     *
     * Default is `3`.
     */
    val maxDigits = objects.property<Int>().apply {
        set(DEFAULT_MAX_DIGITS)
    }

    companion object {
        const val DEFAULT_RELEASE_BUILD_ONLY = true
        const val DEFAULT_REQUIRE_VALID_GIT_TAG = false
        const val DEFAULT_FETCH_TAGS_WHEN_NONE_EXISTS_LOCALLY = false
        const val DEFAULT_MAX_DIGITS = 3
        const val MAX_DIGITS_RANGE_MIN = 1
        const val MAX_DIGITS_RANGE_MAX = 4
    }
}

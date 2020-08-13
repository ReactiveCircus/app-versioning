package io.github.reactivecircus.appversioning

/**
 * Type-safe representation of a version number that follows [Semantic Versioning 2.0.0](https://semver.org/#semantic-versioning-200).
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val buildMetadata: String? = null
) {
    override fun toString() = buildString {
        append("$major.$minor.$patch")
        if (preRelease != null) {
            append("-$preRelease")
        }
        if (buildMetadata != null) {
            append("+$buildMetadata")
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun fromGitTag(gitTag: GitTag, allowPrefixV: Boolean = true): SemVer = gitTag.toSemVer(allowPrefixV)
    }
}

/**
 * Tries to create a [SemVer] from a [GitTag] by parsing its `rawTagName` field.
 * @param allowPrefixV whether prefixing a semantic version with a “v” is allowed.
 * @throws [IllegalArgumentException] when the `rawTagName` of the [GitTag] is not a valid [SemVer].
 */
fun GitTag.toSemVer(allowPrefixV: Boolean = true): SemVer {
    val result = requireNotNull(SEM_VER_REGEX.toRegex().matchEntire(rawTagName)) {
        "$rawTagName is not a valid SemVer."
    }
    val (prefixV, major, minor, patch, preRelease, buildMetadata) = result.destructured
    require(prefixV.isEmpty() || allowPrefixV) {
        "$rawTagName is not a valid SemVer as prefix \"v\" is not allowed unless `allowPrefixV` is set to true."
    }
    return SemVer(
        major = major.toInt(),
        minor = minor.toInt(),
        patch = patch.toInt(),
        preRelease = preRelease.ifEmpty { null },
        buildMetadata = buildMetadata.ifEmpty { null }
    )
}

/**
 * SemVer regex from https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
 * allowing an optional "v" prefix.
 */
private const val SEM_VER_REGEX =
    "^(v)?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?\$"

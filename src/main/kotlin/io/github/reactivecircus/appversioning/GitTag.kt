package io.github.reactivecircus.appversioning

import java.util.Objects

/**
 * Type-safe representation of a git tag.
 */
class GitTag internal constructor(
    val rawTagName: String,
    val commitsSinceLatestTag: Int,
    val commitHash: String,
) {
    /**
     * Parses this tag's [rawTagName] into a [SemVer], stripping [prefix] first if non-empty.
     *
     * @param prefix expected prefix before the semantic version (default is `""`).
     * @throws [IllegalArgumentException] when [rawTagName] does not start with [prefix] or the remaining string is not a valid [SemVer].
     */
    @JvmOverloads
    fun toSemVer(prefix: String = ""): SemVer {
        require(rawTagName.startsWith(prefix)) {
            "Tag \"$rawTagName\" does not start with expected prefix \"$prefix\"."
        }
        return SemVer.parse(rawTagName.removePrefix(prefix))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GitTag) return false
        return rawTagName == other.rawTagName &&
            commitsSinceLatestTag == other.commitsSinceLatestTag &&
            commitHash == other.commitHash
    }

    override fun hashCode(): Int = Objects.hash(rawTagName, commitsSinceLatestTag, commitHash)

    override fun toString(): String = rawTagName
}

/**
 * Parses the output of `git describe --tags --long` into a [GitTag].
 */
internal fun String.toGitTag(): GitTag {
    val result = requireNotNull("(.*)-(\\d+)-g([0-9,a-f]{7,40})\$".toRegex().matchEntire(this)) {
        "$this is not a valid git tag."
    }
    val (rawTagName, commitsSinceLatestTag, commitHash) = result.destructured
    return GitTag(rawTagName, commitsSinceLatestTag.toInt(), commitHash)
}

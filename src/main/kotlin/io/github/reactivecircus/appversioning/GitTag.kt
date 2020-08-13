package io.github.reactivecircus.appversioning

/**
 * Type-safe representation of a git tag.
 */
data class GitTag(
    val rawTagName: String,
    val commitsSinceLatestTag: Int,
    val commitHash: String
) {
    override fun toString() = rawTagName
}

/**
 * Parses the output of `git describe --tags --long` into a [GitTag].
 */
internal fun String.toGitTag(): GitTag {
    val result = requireNotNull("(.*)-(\\d+)-g([0-9,a-f]{7})\$".toRegex().matchEntire(this)) {
        "$this is not a valid git tag."
    }
    val (rawTagName, commitsSinceLatestTag, commitHash) = result.destructured
    return GitTag(rawTagName, commitsSinceLatestTag.toInt(), commitHash)
}

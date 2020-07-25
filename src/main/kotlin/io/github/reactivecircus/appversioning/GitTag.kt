package io.github.reactivecircus.appversioning

// TODO add commitHash, rawTagName etc to GitTag
/**
 * Type-safe representation of a git tag.
 */
class GitTag(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val commitsSinceLatestTag: Int
) {
    override fun toString(): String {
        return "$major.$minor.$patch".let { semVersion ->
            if (commitsSinceLatestTag > 0) {
                "$semVersion.$commitsSinceLatestTag"
            } else {
                semVersion
            }
        }
    }

    companion object {
        val FALLBACK = GitTag(0, 0, 0, 0)
    }
}

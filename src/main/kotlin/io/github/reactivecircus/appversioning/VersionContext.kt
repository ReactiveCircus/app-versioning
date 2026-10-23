package io.github.reactivecircus.appversioning

/**
 * Context available to [VersionCodeSpec] and [VersionNameSpec].
 */
interface VersionContext {
    /**
     * The latest git tag (matching `tagFilter` if provided).
     */
    val tag: GitTag

    /**
     * The build variant the version is generated for.
     */
    val variant: VariantInfo
}

internal class DefaultVersionContext(
    override val tag: GitTag,
    override val variant: VariantInfo,
) : VersionContext

package io.github.reactivecircus.appversioning

import java.util.Objects
import kotlin.math.pow

/**
 * Type-safe representation of a version number that follows [Semantic Versioning 2.0.0](https://semver.org/#semantic-versioning-200).
 */
class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val buildMetadata: String? = null,
) : Comparable<SemVer> {
    /**
     * Generates an integer version code from this [SemVer] using positional notation.
     *
     * @param digitsPerComponent number of digits allocated to the [minor] and [patch] components (default is 2).
     * @throws [IllegalArgumentException] when [digitsPerComponent] is not positive, [minor] or [patch] version is out of range allowed by [digitsPerComponent], or when the result is above [Int.MAX_VALUE].
     */
    @JvmOverloads
    fun toVersionCode(digitsPerComponent: Int = DEFAULT_DIGITS_PER_COMPONENT): Int {
        require(digitsPerComponent > 0)
        val maxComponentValue = 10.0.pow(digitsPerComponent)
        require(patch in 0..<maxComponentValue.toLong())
        require(minor in 0..<maxComponentValue.toLong())
        val result = major * 10.0.pow(digitsPerComponent * 2) + minor * maxComponentValue + patch
        require(result <= Int.MAX_VALUE)
        return result.toInt()
    }

    override fun compareTo(other: SemVer): Int = when {
        major != other.major -> major.compareTo(other.major)
        minor != other.minor -> minor.compareTo(other.minor)
        patch != other.patch -> patch.compareTo(other.patch)
        preRelease == null && other.preRelease == null -> 0
        preRelease == null -> 1
        other.preRelease == null -> -1
        else -> comparePreRelease(preRelease, other.preRelease)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemVer) return false
        return major == other.major &&
            minor == other.minor &&
            patch == other.patch &&
            preRelease == other.preRelease &&
            buildMetadata == other.buildMetadata
    }

    override fun hashCode(): Int = Objects.hash(major, minor, patch, preRelease, buildMetadata)

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (preRelease != null) {
            append("-$preRelease")
        }
        if (buildMetadata != null) {
            append("+$buildMetadata")
        }
    }

    companion object {
        private const val DEFAULT_DIGITS_PER_COMPONENT = 2

        /**
         * Parses [text] into a strict [SemVer] (no prefix allowed).
         *
         * @throws [IllegalArgumentException] when [text] is not a valid [SemVer].
         */
        @JvmStatic
        fun parse(text: String): SemVer = requireNotNull(parseOrNull(text)) {
            "\"$text\" is not a valid SemVer (https://semver.org/#semantic-versioning-200)."
        }

        /**
         * Parses [text] into a strict [SemVer] (no prefix allowed), or returns `null` if [text] is not a valid [SemVer].
         */
        @JvmStatic
        @Suppress("DestructuringDeclarationWithTooManyEntries")
        fun parseOrNull(text: String): SemVer? {
            val (major, minor, patch, preRelease, buildMetadata) =
                SEM_VER_REGEX.matchEntire(text)?.destructured ?: return null
            val majorInt = major.toIntOrNull()
            val minorInt = minor.toIntOrNull()
            val patchInt = patch.toIntOrNull()
            return if (majorInt != null && minorInt != null && patchInt != null) {
                SemVer(
                    major = majorInt,
                    minor = minorInt,
                    patch = patchInt,
                    preRelease = preRelease.ifEmpty { null },
                    buildMetadata = buildMetadata.ifEmpty { null },
                )
            } else {
                null
            }
        }
    }
}

private fun comparePreRelease(a: String, b: String): Int {
    val aParts = a.split('.')
    val bParts = b.split('.')
    return aParts.zip(bParts)
        .firstNotNullOfOrNull { (aPart, bPart) ->
            comparePreReleaseIdentifier(aPart, bPart).takeIf { it != 0 }
        }
        ?: aParts.size.compareTo(bParts.size)
}

private fun comparePreReleaseIdentifier(a: String, b: String): Int {
    val aIsNumeric = a.all { it.isDigit() }
    val bIsNumeric = b.all { it.isDigit() }
    return when {
        a == b -> 0
        aIsNumeric && bIsNumeric -> a.toBigInteger().compareTo(b.toBigInteger())
        aIsNumeric -> -1
        bIsNumeric -> 1
        else -> a.compareTo(b)
    }
}

/**
 * Strict SemVer 2.0.0 regex from https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
 */
private val SEM_VER_REGEX =
    "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?\$".toRegex()

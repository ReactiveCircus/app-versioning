package io.github.reactivecircus.appversioning

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VersionSpecsTest {
    private val variant = VariantInfo(buildType = "release", flavorName = "", variantName = "release")

    @Test
    fun `VersionCodeSpec#SemVer converts SemVer compliant tag to integer using positional notation`() {
        assertEquals(1, evaluateVersionCode("0.0.1"))
        assertEquals(100, evaluateVersionCode("0.1.0"))
        assertEquals(10000, evaluateVersionCode("1.0.0"))
        assertEquals(10099, evaluateVersionCode("1.0.99"))
        assertEquals(19999, evaluateVersionCode("1.99.99"))
        assertEquals(10203, evaluateVersionCode("1.2.3-alpha01+build.567"))
        assertEquals(30000, evaluateVersionCode("v3.0.0-SNAPSHOT"))
    }

    @Test
    fun `VersionCodeSpec#SemVer fails when tag is not SemVer compliant`() {
        val exception = assertFailsWith<IllegalStateException> {
            evaluateVersionCode("1.2")
        }
        assertEquals(
            """
                Could not generate versionCode as "1.2" does not follow semantic versioning.
                Please either ensure latest git tag follows semantic versioning, or provide a custom spec for generating versionCode using the `versionCode` property.
            """.trimIndent(),
            exception.message
        )
    }

    @Test
    fun `VersionCodeSpec#SemVer fails when SemVer component exceeds maximum digits`() {
        val exception = assertFailsWith<IllegalStateException> {
            evaluateVersionCode("1.2.100")
        }
        assertEquals(
            """
                Could not generate versionCode from "1.2.100" as the SemVer cannot be represented as an Integer.
                This is usually because MAJOR or MINOR version is greater than 99, as by default maximum of 2 digits is allowed for MINOR and PATCH components of a SemVer tag.
                Another reason might be that the overall positional notation of the SemVer (MAJOR * 10000 + MINOR * 100 + PATCH) is greater than the maximum value of an integer (2147483647).
                As a workaround you can provide a custom spec for generating versionCode using the `versionCode` property.
            """.trimIndent(),
            exception.message
        )
    }

    @Test
    fun `VersionNameSpec#TagName returns rawTagName`() {
        assertEquals("0.0.1", evaluateVersionName("0.0.1"))
        assertEquals("v1.1.0-alpha01", evaluateVersionName("v1.1.0-alpha01"))
        assertEquals("alpha", evaluateVersionName("alpha"))
    }

    private fun evaluateVersionCode(tagName: String, spec: VersionCodeSpec = VersionCodeSpec.SemVer): Int {
        val context = DefaultVersionContext(GitTag(tagName, 0, "abcd123"), variant)
        return with(spec) { context.versionCode() }
    }

    private fun evaluateVersionName(tagName: String, spec: VersionNameSpec = VersionNameSpec.TagName): String {
        val context = DefaultVersionContext(GitTag(tagName, 0, "abcd123"), variant)
        return with(spec) { context.versionName() }
    }
}

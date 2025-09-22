package io.github.reactivecircus.appversioning

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VariantInfoTest {
    @Test
    fun `VariantInfo#isDebugBuild returns true when builtType is 'debug'`() {
        assertTrue(
            VariantInfo(
                buildType = "debug",
                flavorName = "",
                variantName = "debug"
            ).isDebugBuild
        )

        assertFalse(
            VariantInfo(
                buildType = "release",
                flavorName = "",
                variantName = "release"
            ).isDebugBuild
        )

        assertFalse(
            VariantInfo(
                buildType = null,
                flavorName = "",
                variantName = ""
            ).isDebugBuild
        )

        assertFalse(
            VariantInfo(
                buildType = "DEBUG",
                flavorName = "prod",
                variantName = "prodDebug"
            ).isDebugBuild
        )
    }

    @Test
    fun `VariantInfo#isReleaseBuild returns true when builtType is 'release'`() {
        assertTrue(
            VariantInfo(
                buildType = "release",
                flavorName = "",
                variantName = "release"
            ).isReleaseBuild
        )

        assertFalse(
            VariantInfo(
                buildType = "debug",
                flavorName = "",
                variantName = "debug"
            ).isReleaseBuild
        )

        assertFalse(
            VariantInfo(
                buildType = null,
                flavorName = "",
                variantName = ""
            ).isReleaseBuild
        )

        assertFalse(
            VariantInfo(
                buildType = "RELEASE",
                flavorName = "prod",
                variantName = "prodRelease"
            ).isDebugBuild
        )
    }
}

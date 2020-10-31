package io.github.reactivecircus.appversioning

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VariantInfoTest {

    @Test
    fun `VariantInfo#isDebugBuild returns true when builtType is 'debug'`() {
        assertThat(
            VariantInfo(
                buildType = "debug",
                flavorName = "",
                variantName = "debug"
            ).isDebugBuild
        ).isTrue()

        assertThat(
            VariantInfo(
                buildType = "release",
                flavorName = "",
                variantName = "release"
            ).isDebugBuild
        ).isFalse()

        assertThat(
            VariantInfo(
                buildType = null,
                flavorName = "",
                variantName = ""
            ).isDebugBuild
        ).isFalse()

        assertThat(
            VariantInfo(
                buildType = "DEBUG",
                flavorName = "prod",
                variantName = "prodDebug"
            ).isDebugBuild
        ).isFalse()
    }

    @Test
    fun `VariantInfo#isReleaseBuild returns true when builtType is 'release'`() {
        assertThat(
            VariantInfo(
                buildType = "release",
                flavorName = "",
                variantName = "release"
            ).isReleaseBuild
        ).isTrue()

        assertThat(
            VariantInfo(
                buildType = "debug",
                flavorName = "",
                variantName = "debug"
            ).isReleaseBuild
        ).isFalse()

        assertThat(
            VariantInfo(
                buildType = null,
                flavorName = "",
                variantName = ""
            ).isReleaseBuild
        ).isFalse()

        assertThat(
            VariantInfo(
                buildType = "RELEASE",
                flavorName = "prod",
                variantName = "prodRelease"
            ).isDebugBuild
        ).isFalse()
    }
}

package io.github.reactivecircus.appversioning

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SemVerTest {

    @Test
    fun `SemVer can be converted to integer representation using positional notation`() {
        assertThat(
            SemVer(
                major = 0,
                minor = 0,
                patch = 3
            ).toInt(maxDigitsPerComponent = 2)
        )
            .isEqualTo(3)

        assertThat(
            SemVer(
                major = 0,
                minor = 1,
                patch = 3
            ).toInt(maxDigitsPerComponent = 2)
        )
            .isEqualTo(103)

        assertThat(
            SemVer(
                major = 2,
                minor = 1,
                patch = 3
            ).toInt(maxDigitsPerComponent = 2)
        )
            .isEqualTo(20103)

        assertThat(
            SemVer(
                major = 0,
                minor = 0,
                patch = 99
            ).toInt(maxDigitsPerComponent = 2)
        )
            .isEqualTo(99)

        assertThat(
            SemVer(
                major = 0,
                minor = 99,
                patch = 99
            ).toInt(maxDigitsPerComponent = 2)
        )
            .isEqualTo(9999)

        assertThat(
            SemVer(
                major = 100,
                minor = 99,
                patch = 99
            ).toInt(maxDigitsPerComponent = 2)
        )
            .isEqualTo(1009999)
    }

    @Test
    fun `converting SemVer to integer throws exception when maxDigitsPerComponent is not positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            SemVer(
                major = 1,
                minor = 2,
                patch = 3
            ).toInt(maxDigitsPerComponent = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SemVer(
                major = 1,
                minor = 2,
                patch = 3
            ).toInt(maxDigitsPerComponent = 0)
        }
    }

    @Test
    fun `converting SemVer to integer throws exception when a version component is out of range`() {
        assertThrows(IllegalArgumentException::class.java) {
            SemVer(
                major = 1,
                minor = 2,
                patch = 10
            ).toInt(maxDigitsPerComponent = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SemVer(
                major = 1,
                minor = 10,
                patch = 3
            ).toInt(maxDigitsPerComponent = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SemVer(
                major = 1,
                minor = 2,
                patch = 100
            ).toInt(maxDigitsPerComponent = 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SemVer(
                major = 1,
                minor = 100,
                patch = 3
            ).toInt(maxDigitsPerComponent = 2)
        }
    }

    @Test
    fun `converting SemVer to integer throws exception when result is out of range`() {
        assertThat(
            SemVer(
                major = 0,
                minor = 0,
                patch = Int.MAX_VALUE
            ).toInt(maxDigitsPerComponent = 10)
        ).isEqualTo(Int.MAX_VALUE)

        assertThrows(IllegalArgumentException::class.java) {
            SemVer(
                major = 0,
                minor = 1,
                patch = Int.MAX_VALUE
            ).toInt(maxDigitsPerComponent = 10)
        }
    }

    @Test
    fun `SemVer compliant GitTag can be converted to a SemVer`() {
        assertThat(
            GitTag(
                rawTagName = "0.0.4",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 0,
                minor = 0,
                patch = 4
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.2.3",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 2,
                patch = 3
            )
        )
        assertThat(
            GitTag(
                rawTagName = "10.20.30",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 10,
                minor = 20,
                patch = 30
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.1.2-prerelease+meta",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 1,
                patch = 2,
                preRelease = "prerelease",
                buildMetadata = "meta"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.1.2+meta",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 1,
                patch = 2,
                buildMetadata = "meta"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.1.2+meta-valid",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 1,
                patch = 2,
                buildMetadata = "meta-valid"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-beta",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "beta"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha.beta",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha.beta"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha.beta.1",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha.beta.1"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha.1",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha.1"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha0.valid",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha0.valid"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha.0valid",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha.0valid"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha-a.b-c-somethinglong",
                buildMetadata = "build.1-aef.1-its-okay"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-rc.1+build.1",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "rc.1",
                buildMetadata = "build.1"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha.beta.1",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha.beta.1"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "2.0.0-rc.1+build.123",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 2,
                minor = 0,
                patch = 0,
                preRelease = "rc.1",
                buildMetadata = "build.123"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.2.3-beta",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 2,
                patch = 3,
                preRelease = "beta"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "10.2.3-DEV-SNAPSHOT",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 10,
                minor = 2,
                patch = 3,
                preRelease = "DEV-SNAPSHOT"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.2.3-SNAPSHOT-123",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 2,
                patch = 3,
                preRelease = "SNAPSHOT-123"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0
            )
        )
        assertThat(
            GitTag(
                rawTagName = "2.0.0",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 2,
                minor = 0,
                patch = 0
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.1.7",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 1,
                patch = 7
            )
        )
        assertThat(
            GitTag(
                rawTagName = "2.0.0+build.1848",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 2,
                minor = 0,
                patch = 0,
                buildMetadata = "build.1848"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "2.0.1-alpha.1227",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 2,
                minor = 0,
                patch = 1,
                preRelease = "alpha.1227"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-alpha+beta",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "alpha",
                buildMetadata = "beta"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.2.3----RC-SNAPSHOT.12.9.1--.12+788",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 2,
                patch = 3,
                preRelease = "---RC-SNAPSHOT.12.9.1--.12",
                buildMetadata = "788"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.2.3----R-S.12.9.1--.12+meta",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 2,
                patch = 3,
                preRelease = "---R-S.12.9.1--.12",
                buildMetadata = "meta"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.2.3----RC-SNAPSHOT.12.9.1--.12",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 2,
                patch = 3,
                preRelease = "---RC-SNAPSHOT.12.9.1--.12"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0+0.build.1-rc.10000aaa-kk-0.1",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                buildMetadata = "0.build.1-rc.10000aaa-kk-0.1"
            )
        )
        assertThat(
            GitTag(
                rawTagName = "999999999.99999999.9999999",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 999999999,
                minor = 99999999,
                patch = 9999999
            )
        )
        assertThat(
            GitTag(
                rawTagName = "1.0.0-0A.is.legal",
                commitsSinceLatestTag = 0,
                commitHash = "9c28ad3"
            ).toSemVer()
        ).isEqualTo(
            SemVer(
                major = 1,
                minor = 0,
                patch = 0,
                preRelease = "0A.is.legal"
            )
        )
    }

    @Test
    fun `converting non-SemVer compliant GitTag to SemVer throws exception`() {
        listOf(
            "1",
            "1.2",
            "1.2.3-0123",
            "1.2.3-0123.0123",
            "1.1.2+.123",
            "+invalid",
            "-invalid",
            "-invalid+invalid",
            "-invalid.01",
            "alpha",
            "alpha.beta",
            "alpha.beta.1",
            "alpha.1",
            "alpha+beta",
            "alpha_beta",
            "alpha.",
            "alpha..",
            "beta",
            "1.0.0-alpha_beta",
            "-alpha.",
            "1.0.0-alpha..",
            "1.0.0-alpha..1",
            "1.0.0-alpha...1",
            "1.0.0-alpha....1",
            "1.0.0-alpha.....1",
            "1.0.0-alpha......1",
            "1.0.0-alpha.......1",
            "01.1.1",
            "1.01.1",
            "1.1.01",
            "1.2",
            "1.2.3.DEV",
            "1.2-SNAPSHOT",
            "1.2.31.2.3----RC-SNAPSHOT.12.09.1--..12+788",
            "1.2-RC-SNAPSHOT",
            "-1.0.3-gamma+b7718",
            "+justmeta",
            "9.8.7+meta+meta",
            "9.8.7-whatever+meta+meta",
            "99999999999999999999999.999999999999999999.99999999999999999----RC-SNAPSHOT.12.09.1--------------------------------..12"
        ).forEach { version ->
            assertThrows(IllegalArgumentException::class.java) {
                GitTag(
                    rawTagName = version,
                    commitsSinceLatestTag = 0,
                    commitHash = "9c28ad3"
                ).toSemVer()
            }
        }
    }

    @Test
    fun `SemVer compliant GitTag with a prefix v can be converted to a GitTag when allowPrefixV is true`() {
        assertThat(
            GitTag(
                rawTagName = "v1.2.3",
                commitsSinceLatestTag = 3,
                commitHash = "9c28ad3"
            ).toSemVer(allowPrefixV = true)
        )
            .isEqualTo(
                SemVer(
                    major = 1,
                    minor = 2,
                    patch = 3
                )
            )
    }

    @Test
    fun `converting SemVer compliant GitTag with a prefix v throws exception when allowPrefixV is false`() {
        assertThrows(IllegalArgumentException::class.java) {
            GitTag(
                rawTagName = "v1.2.3",
                commitsSinceLatestTag = 3,
                commitHash = "9c28ad3"
            ).toSemVer(allowPrefixV = false)
        }
    }

    @Test
    fun `SemVer can be created from fromGitTag() companion function`() {
        val gitTag = GitTag(
            rawTagName = "1.2.3",
            commitsSinceLatestTag = 0,
            commitHash = "9c28ad3"
        )
        assertThat(gitTag.toSemVer())
            .isEqualTo(SemVer.fromGitTag(gitTag))
    }

    @Test
    fun `SemVer#toString() returns the SemVer in string representation`() {
        listOf(
            "0.0.4",
            "1.2.3",
            "10.20.30",
            "1.1.2-prerelease+meta",
            "1.1.2+meta",
            "1.1.2+meta-valid",
            "1.0.0-alpha",
            "1.0.0-beta",
            "1.0.0-alpha.beta",
            "1.0.0-alpha.beta.1",
            "1.0.0-alpha.1",
            "1.0.0-alpha0.valid",
            "1.0.0-alpha.0valid",
            "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay",
            "1.0.0-rc.1+build.1",
            "2.0.0-rc.1+build.123",
            "1.2.3-beta",
            "10.2.3-DEV-SNAPSHOT",
            "1.2.3-SNAPSHOT-123",
            "1.0.0",
            "2.0.0",
            "1.1.7",
            "2.0.0+build.1848",
            "2.0.1-alpha.1227",
            "1.0.0-alpha+beta",
            "1.2.3----RC-SNAPSHOT.12.9.1--.12+788",
            "1.2.3----R-S.12.9.1--.12+meta",
            "1.2.3----RC-SNAPSHOT.12.9.1--.12",
            "1.0.0+0.build.1-rc.10000aaa-kk-0.1",
            "999999999.99999999.9999999",
            "1.0.0-0A.is.legal"
        ).forEach { version ->
            assertThat(
                GitTag(
                    rawTagName = version,
                    commitsSinceLatestTag = 0,
                    commitHash = "9c28ad3"
                ).toSemVer().toString()
            ).isEqualTo(version)
        }
    }
}

package io.github.reactivecircus.appversioning

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SemVerTest {
    @Test
    fun `SemVer can be converted to integer representation using positional notation`() {
        assertEquals(3, SemVer(0, 0, 3).toInt(maxDigitsPerComponent = 2))
        assertEquals(103, SemVer(0, 1, 3).toInt(maxDigitsPerComponent = 2))
        assertEquals(20103, SemVer(2, 1, 3).toInt(maxDigitsPerComponent = 2))
        assertEquals(99, SemVer(0, 0, 99).toInt(maxDigitsPerComponent = 2))
        assertEquals(9999, SemVer(0, 99, 99).toInt(maxDigitsPerComponent = 2))
        assertEquals(1009999, SemVer(100, 99, 99).toInt(maxDigitsPerComponent = 2))
    }

    @Test
    fun `converting SemVer to integer throws exception when maxDigitsPerComponent is not positive`() {
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 3).toInt(-1) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 3).toInt(0) }
    }

    @Test
    fun `converting SemVer to integer throws exception when a version component is out of range`() {
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 10).toInt(1) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 10, 3).toInt(1) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 100).toInt(2) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 100, 3).toInt(2) }
    }

    @Test
    fun `converting SemVer to integer throws exception when result is out of range`() {
        assertEquals(Int.MAX_VALUE, SemVer(0, 0, Int.MAX_VALUE).toInt(maxDigitsPerComponent = 10))
        assertFailsWith<IllegalArgumentException> { SemVer(0, 1, Int.MAX_VALUE).toInt(10) }
    }

    @Test
    fun `SemVer compliant GitTag can be converted to a SemVer`() {
        assertEquals(
            SemVer(0, 0, 4),
            GitTag("0.0.4", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 2, 3),
            GitTag("1.2.3", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(10, 20, 30),
            GitTag("10.20.30", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 1, 2, "prerelease", "meta"),
            GitTag("1.1.2-prerelease+meta", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 1, 2, buildMetadata = "meta"),
            GitTag("1.1.2+meta", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 1, 2, buildMetadata = "meta-valid"),
            GitTag("1.1.2+meta-valid", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha"),
            GitTag("1.0.0-alpha", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "beta"),
            GitTag("1.0.0-beta", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha.beta"),
            GitTag("1.0.0-alpha.beta", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha.beta.1"),
            GitTag("1.0.0-alpha.beta.1", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha.1"),
            GitTag("1.0.0-alpha.1", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha0.valid"),
            GitTag("1.0.0-alpha0.valid", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha.0valid"),
            GitTag("1.0.0-alpha.0valid", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha-a.b-c-somethinglong", buildMetadata = "build.1-aef.1-its-okay"),
            GitTag("1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "rc.1", buildMetadata = "build.1"),
            GitTag("1.0.0-rc.1+build.1", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha.beta.1"),
            GitTag("1.0.0-alpha.beta.1", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(2, 0, 0, preRelease = "rc.1", buildMetadata = "build.123"),
            GitTag("2.0.0-rc.1+build.123", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 2, 3, preRelease = "beta"),
            GitTag("1.2.3-beta", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(10, 2, 3, preRelease = "DEV-SNAPSHOT"),
            GitTag("10.2.3-DEV-SNAPSHOT", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 2, 3, preRelease = "SNAPSHOT-123"),
            GitTag("1.2.3-SNAPSHOT-123", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0),
            GitTag("1.0.0", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(2, 0, 0),
            GitTag("2.0.0", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 1, 7),
            GitTag("1.1.7", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(2, 0, 0, buildMetadata = "build.1848"),
            GitTag("2.0.0+build.1848", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(2, 0, 1, preRelease = "alpha.1227"),
            GitTag("2.0.1-alpha.1227", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "alpha", buildMetadata = "beta"),
            GitTag("1.0.0-alpha+beta", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 2, 3, preRelease = "---RC-SNAPSHOT.12.9.1--.12", buildMetadata = "788"),
            GitTag("1.2.3----RC-SNAPSHOT.12.9.1--.12+788", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 2, 3, preRelease = "---R-S.12.9.1--.12", buildMetadata = "meta"),
            GitTag("1.2.3----R-S.12.9.1--.12+meta", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 2, 3, preRelease = "---RC-SNAPSHOT.12.9.1--.12"),
            GitTag("1.2.3----RC-SNAPSHOT.12.9.1--.12", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, buildMetadata = "0.build.1-rc.10000aaa-kk-0.1"),
            GitTag("1.0.0+0.build.1-rc.10000aaa-kk-0.1", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(999999999, 99999999, 9999999),
            GitTag("999999999.99999999.9999999", 0, "9c28ad3").toSemVer()
        )
        assertEquals(
            SemVer(1, 0, 0, preRelease = "0A.is.legal"),
            GitTag("1.0.0-0A.is.legal", 0, "9c28ad3").toSemVer()
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
            assertFailsWith<IllegalArgumentException> { GitTag(version, 0, "9c28ad3").toSemVer() }
        }
    }

    @Test
    fun `SemVer compliant GitTag with a prefix v can be converted to a GitTag when allowPrefixV is true`() {
        assertEquals(SemVer(1, 2, 3), GitTag("v1.2.3", 3, "9c28ad3").toSemVer(allowPrefixV = true))
    }

    @Test
    fun `converting SemVer compliant GitTag with a prefix v throws exception when allowPrefixV is false`() {
        assertFailsWith<IllegalArgumentException> { GitTag("v1.2.3", 3, "9c28ad3").toSemVer(allowPrefixV = false) }
    }

    @Test
    fun `SemVer can be created from fromGitTag() companion function`() {
        val gitTag = GitTag("1.2.3", 0, "9c28ad3")
        assertEquals(SemVer.fromGitTag(gitTag), gitTag.toSemVer())
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
            assertEquals(version, GitTag(version, 0, "9c28ad3").toSemVer().toString())
        }
    }
}

package io.github.reactivecircus.appversioning

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerTest {
    @Test
    fun `SemVer can be converted to versionCode using positional notation`() {
        assertEquals(3, SemVer(0, 0, 3).toVersionCode())
        assertEquals(103, SemVer(0, 1, 3).toVersionCode())
        assertEquals(20103, SemVer(2, 1, 3).toVersionCode())
        assertEquals(99, SemVer(0, 0, 99).toVersionCode())
        assertEquals(9999, SemVer(0, 99, 99).toVersionCode())
        assertEquals(1009999, SemVer(100, 99, 99).toVersionCode())
        assertEquals(1002003, SemVer(1, 2, 3).toVersionCode(digitsPerComponent = 3))
    }

    @Test
    fun `toVersionCode throws exception when digitsPerComponent is not positive`() {
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 3).toVersionCode(digitsPerComponent = -1) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 3).toVersionCode(digitsPerComponent = 0) }
    }

    @Test
    fun `toVersionCode throws exception when a version component is out of range`() {
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 10).toVersionCode(digitsPerComponent = 1) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 10, 3).toVersionCode(digitsPerComponent = 1) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 2, 100).toVersionCode(digitsPerComponent = 2) }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 100, 3).toVersionCode(digitsPerComponent = 2) }
    }

    @Test
    fun `toVersionCode throws exception when result is out of range`() {
        assertEquals(Int.MAX_VALUE, SemVer(0, 0, Int.MAX_VALUE).toVersionCode(digitsPerComponent = 10))
        assertFailsWith<IllegalArgumentException> { SemVer(0, 1, Int.MAX_VALUE).toVersionCode(digitsPerComponent = 10) }
    }

    @Test
    fun `SemVer#parse and SemVer#parseOrNull parse valid strict SemVer strings`() {
        val validSemVers = mapOf(
            "0.0.4" to SemVer(0, 0, 4),
            "1.2.3" to SemVer(1, 2, 3),
            "10.20.30" to SemVer(10, 20, 30),
            "1.1.2-prerelease+meta" to SemVer(1, 1, 2, "prerelease", "meta"),
            "1.1.2+meta" to SemVer(1, 1, 2, buildMetadata = "meta"),
            "1.1.2+meta-valid" to SemVer(1, 1, 2, buildMetadata = "meta-valid"),
            "1.0.0-alpha" to SemVer(1, 0, 0, preRelease = "alpha"),
            "1.0.0-beta" to SemVer(1, 0, 0, preRelease = "beta"),
            "1.0.0-alpha.beta" to SemVer(1, 0, 0, preRelease = "alpha.beta"),
            "1.0.0-alpha.beta.1" to SemVer(1, 0, 0, preRelease = "alpha.beta.1"),
            "1.0.0-alpha.1" to SemVer(1, 0, 0, preRelease = "alpha.1"),
            "1.0.0-alpha0.valid" to SemVer(1, 0, 0, preRelease = "alpha0.valid"),
            "1.0.0-alpha.0valid" to SemVer(1, 0, 0, preRelease = "alpha.0valid"),
            "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay" to SemVer(
                1,
                0,
                0,
                preRelease = "alpha-a.b-c-somethinglong",
                buildMetadata = "build.1-aef.1-its-okay",
            ),
            "1.0.0-rc.1+build.1" to SemVer(1, 0, 0, preRelease = "rc.1", buildMetadata = "build.1"),
            "2.0.0-rc.1+build.123" to SemVer(2, 0, 0, preRelease = "rc.1", buildMetadata = "build.123"),
            "1.2.3-beta" to SemVer(1, 2, 3, preRelease = "beta"),
            "10.2.3-DEV-SNAPSHOT" to SemVer(10, 2, 3, preRelease = "DEV-SNAPSHOT"),
            "1.2.3-SNAPSHOT-123" to SemVer(1, 2, 3, preRelease = "SNAPSHOT-123"),
            "1.0.0" to SemVer(1, 0, 0),
            "2.0.0" to SemVer(2, 0, 0),
            "1.1.7" to SemVer(1, 1, 7),
            "2.0.0+build.1848" to SemVer(2, 0, 0, buildMetadata = "build.1848"),
            "2.0.1-alpha.1227" to SemVer(2, 0, 1, preRelease = "alpha.1227"),
            "1.0.0-alpha+beta" to SemVer(1, 0, 0, preRelease = "alpha", buildMetadata = "beta"),
            "1.2.3----RC-SNAPSHOT.12.9.1--.12+788" to SemVer(
                1,
                2,
                3,
                preRelease = "---RC-SNAPSHOT.12.9.1--.12",
                buildMetadata = "788",
            ),
            "1.2.3----R-S.12.9.1--.12+meta" to SemVer(
                1,
                2,
                3,
                preRelease = "---R-S.12.9.1--.12",
                buildMetadata = "meta",
            ),
            "1.2.3----RC-SNAPSHOT.12.9.1--.12" to SemVer(1, 2, 3, preRelease = "---RC-SNAPSHOT.12.9.1--.12"),
            "1.0.0+0.build.1-rc.10000aaa-kk-0.1" to SemVer(
                1,
                0,
                0,
                buildMetadata = "0.build.1-rc.10000aaa-kk-0.1",
            ),
            "999999999.99999999.9999999" to SemVer(999999999, 99999999, 9999999),
            "1.0.0-0A.is.legal" to SemVer(1, 0, 0, preRelease = "0A.is.legal"),
        )
        validSemVers.forEach { (text, expected) ->
            assertEquals(expected, SemVer.parse(text))
            assertEquals(expected, SemVer.parseOrNull(text))
            assertEquals(expected, GitTag(text, 0, "9c28ad3").toSemVer())
        }
    }

    @Test
    fun `SemVer#parse throws and SemVer#parseOrNull returns null for non-SemVer strings`() {
        listOf(
            "1",
            "1.2",
            "v1.2.3",
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
            "1.2.3.DEV",
            "1.2-SNAPSHOT",
            "1.2.31.2.3----RC-SNAPSHOT.12.09.1--..12+788",
            "1.2-RC-SNAPSHOT",
            "-1.0.3-gamma+b7718",
            "+justmeta",
            "9.8.7+meta+meta",
            "9.8.7-whatever+meta+meta",
            "99999999999999999999999.999999999999999999.99999999999999999----RC-SNAPSHOT.12.09.1--------------------------------..12",
        ).forEach { version ->
            assertNull(SemVer.parseOrNull(version))
            assertFailsWith<IllegalArgumentException> { SemVer.parse(version) }
            assertFailsWith<IllegalArgumentException> { GitTag(version, 0, "9c28ad3").toSemVer() }
        }
    }

    @Test
    fun `GitTag#toSemVer strips prefix when prefix matches and throws when prefix does not match`() {
        assertEquals(SemVer(1, 2, 3), GitTag("v1.2.3", 3, "9c28ad3").toSemVer(prefix = "v"))
        assertEquals(
            SemVer(1, 2, 3, preRelease = "rc01"),
            GitTag("android-1.2.3-rc01", 0, "9c28ad3").toSemVer(prefix = "android-"),
        )
        assertFailsWith<IllegalArgumentException> {
            GitTag("1.2.3", 0, "9c28ad3").toSemVer(prefix = "v")
        }
        assertFailsWith<IllegalArgumentException> {
            GitTag("ios-1.2.3", 0, "9c28ad3").toSemVer(prefix = "android-")
        }
    }

    @Test
    fun `SemVer implements Comparable following SemVer 2_0_0 precedence`() {
        val ordered = listOf(
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-alpha.beta",
            "1.0.0-beta",
            "1.0.0-beta.2",
            "1.0.0-beta.11",
            "1.0.0-rc.1",
            "1.0.0",
            "2.0.0",
            "2.1.0",
            "2.1.1",
        ).map(SemVer::parse)

        for (i in 0..<ordered.lastIndex) {
            assertTrue(ordered[i] < ordered[i + 1], "Expected ${ordered[i]} < ${ordered[i + 1]}")
            assertTrue(ordered[i + 1] > ordered[i], "Expected ${ordered[i + 1]} > ${ordered[i]}")
        }

        // Build metadata does not affect version precedence
        assertEquals(0, SemVer.parse("1.0.0+build1").compareTo(SemVer.parse("1.0.0+build2")))
        assertNotEquals(SemVer.parse("1.0.0+build1"), SemVer.parse("1.0.0+build2"))
    }

    @Test
    fun `SemVer#toString() returns the SemVer in canonical string representation`() {
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
            "1.0.0-0A.is.legal",
        ).forEach { version ->
            assertEquals(version, SemVer.parse(version).toString())
        }
    }
}

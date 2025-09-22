package io.github.reactivecircus.appversioning

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GitTagTest {
    @Test
    fun `valid tag description can be converted to a GitTag`() {
        assertEquals(GitTag("0.1.0", 3, "9c28ad3"), "0.1.0-3-g9c28ad3".toGitTag())
        assertEquals(GitTag("0.1.0", 0, "9c28ad3"), "0.1.0-0-g9c28ad3".toGitTag())
        assertEquals(GitTag("1.0", 20, "36e7453"), "1.0-20-g36e7453".toGitTag())
        assertEquals(GitTag("1.0.0-alpha03", 20, "36e7453"), "1.0.0-alpha03-20-g36e7453".toGitTag())
        assertEquals(GitTag("1.0.0-alpha03", 20, "36e7453"), "1.0.0-alpha03-20-g36e7453".toGitTag())
        assertEquals(GitTag("1.0.0-alpha+001", 20, "36e7453"), "1.0.0-alpha+001-20-g36e7453".toGitTag())
    }

    @Test
    fun `converting invalid tag description to GitTag throws exception`() {
        listOf(
            "1.0.0-g36e7453",
            "1.0.0-a-g36e7453",
            "1.0.0-a3-g36e7453",
            "1.0.0-3-36e7453",
            "1.0.0-3-g36e745",
            "1.0.0-a-g36e745g"
        ).forEach { tagDescription ->
            assertFailsWith<IllegalArgumentException> { tagDescription.toGitTag() }
        }
    }

    @Test
    fun `GitTag#toString() returns rawTagName`() {
        val gitTag = GitTag("1.0.0-alpha03", 1, "36e7453")
        assertEquals("1.0.0-alpha03", gitTag.toString())
    }
}

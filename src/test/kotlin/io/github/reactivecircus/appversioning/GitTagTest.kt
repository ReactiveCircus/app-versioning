package io.github.reactivecircus.appversioning

import com.google.common.truth.Truth.assertThat
import org.gradle.internal.impldep.org.junit.Assert.assertThrows
import org.junit.Test

class GitTagTest {

    @Test
    fun `valid tag description can be converted to a GitTag`() {
        assertThat("0.1.0-3-g9c28ad3".toGitTag())
            .isEqualTo(
                GitTag(
                    rawTagName = "0.1.0",
                    commitsSinceLatestTag = 3,
                    commitHash = "9c28ad3"
                )
            )
        assertThat("0.1.0-0-g9c28ad3".toGitTag())
            .isEqualTo(
                GitTag(
                    rawTagName = "0.1.0",
                    commitsSinceLatestTag = 0,
                    commitHash = "9c28ad3"
                )
            )
        assertThat("1.0-20-g36e7453".toGitTag())
            .isEqualTo(
                GitTag(
                    rawTagName = "1.0",
                    commitsSinceLatestTag = 20,
                    commitHash = "36e7453"
                )
            )
        assertThat("1.0.0-alpha03-20-g36e7453".toGitTag())
            .isEqualTo(
                GitTag(
                    rawTagName = "1.0.0-alpha03",
                    commitsSinceLatestTag = 20,
                    commitHash = "36e7453"
                )
            )
        assertThat("1.0.0-alpha03-20-g36e7453".toGitTag())
            .isEqualTo(
                GitTag(
                    rawTagName = "1.0.0-alpha03",
                    commitsSinceLatestTag = 20,
                    commitHash = "36e7453"
                )
            )
        assertThat("1.0.0-alpha+001-20-g36e7453".toGitTag())
            .isEqualTo(
                GitTag(
                    rawTagName = "1.0.0-alpha+001",
                    commitsSinceLatestTag = 20,
                    commitHash = "36e7453"
                )
            )
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
            assertThrows(IllegalArgumentException::class.java) {
                tagDescription.toGitTag()
            }
        }
    }

    @Test
    fun `GitTag#toString() returns rawTagName`() {
        val gitTag = GitTag(
            rawTagName = "1.0.0-alpha03",
            commitsSinceLatestTag = 1,
            commitHash = "36e7453"
        )
        assertThat(gitTag.toString())
            .isEqualTo(
                "1.0.0-alpha03"
            )
    }
}

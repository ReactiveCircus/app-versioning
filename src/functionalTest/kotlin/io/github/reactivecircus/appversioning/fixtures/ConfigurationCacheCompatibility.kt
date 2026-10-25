package io.github.reactivecircus.appversioning.fixtures

import com.android.build.api.AndroidPluginVersion
import org.gradle.testkit.runner.BuildResult
import kotlin.test.assertTrue

fun currentAgpSupportsConfigurationCache(): Boolean {
    return fixtureAgpVersion >= MIN_CC_COMPLIANT_AGP_VERSION
}

private val fixtureAgpVersion: AndroidPluginVersion by lazy {
    parseAgpVersion(System.getProperty("fixtureAgpVersion"))
}

private fun parseAgpVersion(versionString: String): AndroidPluginVersion {
    val (major, minor, micro) = versionString
        .substringBefore('-')
        .split('.')
        .map { it.toInt() }
    val baseVersion = AndroidPluginVersion(major, minor, micro)
    val preRelease = versionString.substringAfter('-', missingDelimiterValue = "")
    return when {
        preRelease.isEmpty() -> baseVersion
        preRelease.startsWith("alpha") -> baseVersion.alpha(preRelease.removePrefix("alpha").toInt())
        preRelease.startsWith("beta") -> baseVersion.beta(preRelease.removePrefix("beta").toInt())
        preRelease.startsWith("rc") -> baseVersion.rc(preRelease.removePrefix("rc").toInt())
        preRelease == "dev" -> baseVersion.dev()
        else -> error("Unsupported AGP pre-release version: $versionString")
    }
}

private val MIN_CC_COMPLIANT_AGP_VERSION = AndroidPluginVersion(9, 2, 0)

fun BuildResult.assertHasConfigurationCacheEntryLog(
    expectedEntryLog: ConfigurationCacheEntryLog
) {
    if (!currentAgpSupportsConfigurationCache()) return
    assertTrue {
        when (expectedEntryLog) {
            ConfigurationCacheEntryLog.Stored -> output.contains("Configuration cache entry stored.")
            ConfigurationCacheEntryLog.Reused -> output.contains("Reusing configuration cache.")
        }
    }
}

enum class ConfigurationCacheEntryLog {
    Stored,
    Reused,
}

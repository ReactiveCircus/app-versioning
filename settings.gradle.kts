rootProject.name = "app-versioning"

enableFeaturePreview("NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS")

pluginManagement {
    repositories {
        gradlePluginPortal {
            content {
                includeGroupByRegex("org.gradle.*")
            }
        }
        mavenCentral()
    }

    val toolchainsResolverVersion = file("$rootDir/gradle/libs.versions.toml")
        .readLines()
        .first { it.contains("toolchainsResolver") }
        .substringAfter("=")
        .trim()
        .removeSurrounding("\"")

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version toolchainsResolverVersion
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

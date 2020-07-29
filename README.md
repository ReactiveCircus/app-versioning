# App Versioning

![CI](https://github.com/ReactiveCircus/app-versioning/workflows/CI/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.reactivecircus.appversioning/app-versioning-gradle-plugin/badge.svg)](https://search.maven.org/search?q=g:io.github.reactivecircus.appversioning)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Gradle Plugin for lazily generating Android app's `versionCode` & `versionName` from Git tags.

**Minimum version of Android Gradle Plugin required is `4.0.0`.**

More details coming :soon:

## Installation

The **Android App Versioning Gradle Plugin** is available from both **Maven Central** and **Gradle Plugin Portal**. Make sure your top-level `build.gradle` has either `mavenCentral()` or `gradlePluginPortal()` defined in the `buildscript` block:

```groovy
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

The plugin can now be applied to your **Android Application** module (Gradle subproject).

If you use Groovy DSL (`build.gradle`):

```groovy
plugins {
    id 'com.android.application'
    id 'io.github.reactivecircus.app-versioning' version "x.y.z"
}
```

If you use Kotlin DSL (`build.gradle.kts`):

```kotlin
plugins {
    id("com.android.application")
    id("io.github.reactivecircus.app-versioning") version "x.y.z"
}
```

The `version` can be omitted by adding a classpath dependency in the `buildscript` block within the top-level `build.gradle`:

```groovy
buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    dependencies {
        classpath "io.github.reactivecircus.appversioning:app-versioning-gradle-plugin:x.y.z"
    }
}
```

## Configurations

The following work in both Groovy and Kotlin DSL (some of them will change):

```kotlin
appVersioning {
    /**
     * Whether to only generate version name and version code for `release` builds.
     *
     * Default is `true`.
     */
    releaseBuildOnly.set(true)

    /**
     * Whether a valid git tag is required.
     * When set to `true` a git tag in the MAJOR.MINOR.PATCH format must be present.
     * When set to `false` version name "0.0.0" and version code 0 will be used if no valid git tag exists.
     *
     * Default is `false`.
     */
    requireValidGitTag.set(false)

    /**
     * Whether to fetch git tags from remote when no valid git tag can be found locally.
     *
     * Default is `false`.
     */
    fetchTagsWhenNoneExistsLocally.set(false)

    /**
     * Provides a custom rule for generating versionCode by implementing a [GitTag], [ProviderFactory] -> Int lambda.
     * The [GitTag] is computed lazily by the plugin during task execution, whereas the [ProviderFactory] can be used for fetching
     * environment variables, Gradle and system properties.
     *
     * This is useful if you want to fully customize how the versionCode is generated.
     * If not specified, versionCode will be computed from the latest git tag that follows semantic versioning.
     */
    overrideVersionCode { _, _ ->
        // use timestamp as versionCode
        Instant.now().epochSecond.toInt()
    }

    /**
     * Provides a custom rule for generating versionName by implementing a [GitTag], [ProviderFactory] -> String lambda.
     * The [GitTag] is computed lazily by the plugin during task execution, whereas the [ProviderFactory] can be used for fetching
     * environment variables, Gradle and system properties.
     *
     * This is useful if you want to fully customize how the versionName is generated.
     * If not specified, versionName will be the name of the latest git tag.
     */
    overrideVersionName { gitTag, providers ->
        // a custom versionName combining the git tag with an environment variable or system property
        val buildNumber = providers
            .environmentVariable("BUILD_NUMBER")
            .getOrElse("0").toInt()
        "$gitTag - #$buildNumber"
    }
}
```

More samples will be added later.

## Gradle tasks

Plugin currently offers 2 Gradle tasks:

- `generateAppVersionInfoFor<BuildVariant>` - generates the `versionCode` and `versionName` for the `BuildVariant`. This task will be automatically run when building the APK or AAB e.g. by running `assemble<BuildVariant>` or `bundle<BuildVariant>`, and the generated `versionCode` and `versionName` will be injected into the final merged `AndroidManifest`.
- `printAppVersionInfoFor<BuildVariant>` - prints the generated `versionCode` and `versionName` to the console. This task depends on `generateAppVersionInfoFor<BuildVariant>`. 

## License

```
Copyright 2020 Yang Chen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

# Migrating to App Versioning 2.0

This guide covers the breaking changes and new APIs in **App Versioning 2.0** when upgrading from **1.x**.

## Minimum requirements

- **Android Gradle Plugin**: **9.0.0** or higher.
- **Java**: Targets **JVM 17** bytecode.

## Spec APIs

The `overrideVersionCode` and `overrideVersionName` lambdas have been replaced by `versionCode` and `versionName` spec properties:

- `versionCode: Property<VersionCodeSpec>` (defaults to `VersionCodeSpec.SemVer`)
- `versionName: Property<VersionNameSpec>` (defaults to `VersionNameSpec.TagName`)

Both `VersionCodeSpec` and `VersionNameSpec` are SAM (`fun interface`) types with `VersionContext` (`tag: GitTag`, `variant: VariantInfo`) as the implicit receiver.

### Kotlin DSL

**1.x**

```kt
import io.github.reactivecircus.appversioning.toSemVer

appVersioning {
    overrideVersionCode { gitTag, _, variantInfo ->
        val semVer = gitTag.toSemVer()
        val offset = if (variantInfo.flavorName == "paid") 1 else 0
        semVer.major * 10000 + semVer.minor * 100 + semVer.patch + offset
    }
    overrideVersionName { gitTag, _, variantInfo ->
        val suffix = if (variantInfo.isDebugBuild) " (${variantInfo.variantName})" else ""
        "${gitTag.rawTagName} (${gitTag.commitHash})$suffix"
    }
}
```

**2.0**

```kt
appVersioning {
    versionCode.set {
        val offset = if (variant.flavorName == "paid") 1 else 0
        tag.toSemVer().toVersionCode() + offset
    }
    versionName.set {
        val suffix = if (variant.isDebugBuild) " (${variant.variantName})" else ""
        "${tag.name} (${tag.commitHash})$suffix"
    }
}
```

### Groovy DSL

In Groovy, `AppVersioningExtension` provides `versionCode { ... }` and `versionName { ... }` convenience methods that configure a `Closure` with `VersionContext` as its delegate:

**1.x**

```groovy
import io.github.reactivecircus.appversioning.SemVer

appVersioning {
    overrideVersionCode { gitTag, providers, variantInfo ->
        def semVer = SemVer.fromGitTag(gitTag)
        def offset = variantInfo.flavorName == "paid" ? 1 : 0
        semVer.major * 10000 + semVer.minor * 100 + semVer.patch + offset
    }
    overrideVersionName { gitTag, providers, variantInfo ->
        def suffix = variantInfo.debugBuild ? " (${variantInfo.variantName})" : ""
        "${gitTag.rawTagName} (${gitTag.commitHash})${suffix}"
    }
}
```

**2.0**

```groovy
appVersioning {
    versionCode {
        def offset = variant.flavorName == "paid" ? 1 : 0
        tag.toSemVer().toVersionCode() + offset
    }
    versionName {
        def suffix = variant.debugBuild ? " (${variant.variantName})" : ""
        "${tag.name} (${tag.commitHash})${suffix}"
    }
}
```

## Using `ProviderFactory`

In 1.0 `ProviderFactory` was passed into the version code / name computation lambda as a parameter, so external values (e.g., `BUILD_NUMBER` from environment variables) could be read inside the lambda. This was problematic because:

- Gradle computes a task's up-to-date fingerprint and build-cache key **before** task execution, using only declared task input properties.
- If a lambda calls `providers.environmentVariable("BUILD_NUMBER").get()` during execution, Gradle never sees that read when fingerprinting inputs.
- If `BUILD_NUMBER` changes on the next build while `.git` does not, Gradle will wrongly mark the task `UP-TO-DATE` (or restore `FROM-CACHE`) with the stale value, preventing the `GenerateAppVersionInfo` task from executing to produce the new version code / name.

In 2.0 `ProviderFactory` is no longer available in the lambda (via the `VersionContext` receiver type).
Instead, use Gradle's [Provider chaining APIs](https://docs.gradle.org/current/userguide/properties_providers.html#chaining_providers) to lazily map the external values provider to the `VersionCodeSpec` or `VersionNameSpec` property.

### Kotlin DSL

**1.x**

```kt
appVersioning {
    overrideVersionName { gitTag, providers, _ ->
        val buildNumber = providers
            .environmentVariable("BUILD_NUMBER")
            .getOrElse("0")
        "$gitTag (#$buildNumber - ${gitTag.commitHash})"
    }
}
```

**2.0**

```kt
import io.github.reactivecircus.appversioning.VersionNameSpec

val buildNumber = providers.environmentVariable("BUILD_NUMBER").orElse("0")

appVersioning {
    versionName.set(
        buildNumber.map { buildNum ->
            VersionNameSpec {
                "$tag (#$buildNum - ${tag.commitHash})"
            }
        }
    )
}
```

### Groovy DSL

**1.x**

```groovy
appVersioning {
    overrideVersionName { gitTag, providers, variantInfo ->
        def buildNumber = providers
            .environmentVariable("BUILD_NUMBER")
            .getOrElse("0")
        "$gitTag (#$buildNumber - ${gitTag.commitHash})".toString()
    }
}
```

**2.0**

```groovy
import io.github.reactivecircus.appversioning.VersionNameSpec

def buildNumber = providers.environmentVariable("BUILD_NUMBER").orElse("0")

appVersioning {
    versionName.set(
        buildNumber.map { buildNum ->
            { "$tag (#$buildNum - ${tag.commitHash})" } as VersionNameSpec
        }
    )
}
```

## New `GitTag` and `SemVer` APIs

There are a few API changes to `GitTag` and `SemVer` to make SemVer-based version code customization easier.

### Creating `SemVer` from `GitTag`

Both the `fun GitTag.toSemVer(allowPrefixV: Boolean = true): SemVer` extension function and the `fun fromGitTag(gitTag: GitTag, allowPrefixV: Boolean = true): SemVer` factory function have been removed.

Instead, use the new member function on `GitTag`:

```kt
fun toSemVer(prefix: String = ""): SemVer
```

Note that SemVer parsing is now strict, meaning `"v"` prefix is no longer automatically stripped.

Use `prefix` to strip any characters from the start of the tag name before SemVer parsing:

```kt
// For tags like "v1.2.3"
tag.toSemVer(prefix = "v")

// For tags like "android-1.2.3"
tag.toSemVer(prefix = "android-")
```

### Creating `SemVer` from raw strings

If your `GitTag` cannot be directly parsed to `SemVer` using `toSemVer(prefix)`, we now offer APIs for parsing arbitrary strings to `SemVer`.

For example, if your tag name is `"1.2.3-android"`, you can manually strip off the `"-android"` suffix before parsing:

```kt
val semVer = SemVer.parse(tag.name.removeSuffix("-android"))
val semVerOrNull = SemVer.parseOrNull(tag.name.removeSuffix("-android"))
```

### Converting `SemVer` to version code

Previously to customize how SemVer tags were converted to version codes, you had to reimplement the positional notation formula from the SemVer components, even when you just wanted to add a fixed offset to the default SemVer-based version code.

Now you can use the new `SemVer.toVersionCode(digitsPerComponent: Int = 2): Int` API:

```kt
// 1.x
val semVer = gitTag.toSemVer()
semVer.major * 1000000 + semVer.minor * 1000 + semVer.patch + offset

// 2.0
tag.toSemVer().toVersionCode(digitsPerComponent = 3) + offset
```

### Other notable API changes

- `rawTagName` has been renamed to `name`.
- `GitTag` and `SemVer` are no longer data classes.
- `SemVer` now implements `Comparable`.

## Migrating away from timestamp-based `versionCode`

Using `Instant.now().epochSecond.toInt()` for `versionCode` is no longer recommended:

1. Epoch-second timestamps will hit Google Play's maximum allowed `versionCode` of **2,100,000,000** in 2036, and Play Console warns when `versionCode` jumps by large increments.
2. Calling `Instant.now()` inside a spec produces a different value whenever the spec is evaluated.

If your app previously published epoch-second version codes (e.g. `~1,700,000,000`), you can migrate to SemVer-based version codes by adding a fixed offset above your last published `versionCode`:

```kt
appVersioning {
    versionCode.set {
        tag.toSemVer().toVersionCode() + 1_800_000_000
    }
}
```

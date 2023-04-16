# Change Log

## 1.3.1

### Fixed
- Fix an issue where the version info files cannot be found when configuration cache is on - [#30](https://github.com/ReactiveCircus/app-versioning/pull/30)

## 1.3.0

### Changed
- Compile with AGP 8.0.0.
- Compile with Kotlin 1.8.20.

## 1.2.0

### Fixed
- Stop depending on `com.android.tools:sdk-common` to support AGP 8.

### Changed
- Compile with AGP 7.3.1.
- Compile with Kotlin 1.7.20.

## 1.1.2

### Fixed
- Fix an issue where changing git HEAD does not invalidate task cache - [#28](https://github.com/ReactiveCircus/app-versioning/pull/28)

### Changed
- Compile with AGP 7.1.3.
- Compile with Kotlin 1.6.21.

## 1.1.1

Same as 1.1.0.

## 1.1.0

### Added
- Support specifying **bare** git repository e.g. `app.git`.

### Changed
- Compile with AGP 7.0.3.

## 1.0.0

This is our first stable release. Thanks everyone for trying out the plugin and sending bug reports and feature requests!

### Changed
- Compile with AGP 7.0.0.

## 1.0.0-rc01

### Changed
- Minimum Android Gradle Plugin version is now **7.0.0-beta04**.

## 0.10.0

This is the final release of the plugin that's compatible with **Android Gradle Plugin 4.2**.

When AGP **7.0.0-beta01** was released, we thought all the APIs we use are stable and can therefore support **4.2.1** which have the same APIs we were using.
Unfortunately **AGP 7.0.0-beta04** moved `ApplicationAndroidComponentsExtension` to a new package and deprecated the old one. In order to move to the new `ApplicationAndroidComponentsExtension`
before our 1.0 release and avoid the overhead of publishing multiple artifacts, we decided to start requiring **AGP 7.0.0-beta04** in the next release.

### Changed
- Compile with AGP 7.0.0-rc01.
- Compile with Kotlin 1.5.21.

## 0.9.1

### Changed
- Minimum Gradle version is **6.8**.

## 0.9.0

### Changed
- Minimum Android Gradle Plugin version is now **4.2.1**. All versions of **AGP 7.x** are supported.
- Compile with AGP 7.0.0-beta03.
- Compile with Kotlin 1.5.10.

## 0.8.1

### Added
- Support setting git root directory explicitly when the root Gradle project is not the git root.

## 0.8.0

### Added
- Support tag filtering with custom glob pattern.

### Changed
- Compile with AGP 4.2.0-beta02.
- Target Java 11 bytecode.
- Kotlin 1.4.21.
- Gradle 6.8-rc-4.

## 0.7.0

### Changed
- Change minimum Android Gradle Plugin version to **4.2.0-beta01**.
- Support AGP 7.0.0-alpha02.
- Kotlin 1.4.20.
- Gradle 6.8-rc-1.

## 0.6.0

### Added
- Add `VariantInfo` lambda parameters to `overrideVersionVode` and `overrideVersionName` to support customizing `versionCode` and `versionName` based on build variants.

### Changed
- Change `AppVersioningPlugin` from `internal` to `public` to support type-safe plugin application in `buildSrc`.
- AGP 4.2.0-alpha16.
- Kotlin 1.4.20-RC.

### Fixed
- Disable IR to support applying the plugin from `buildSrc`.

## 0.5.0

The plugin now requires the latest version of Android Gradle Plugin (currently `4.2.0-alpha13`) until the next variant APIs become stable.

Please use version `0.4.0` if you want to use the plugin with AGP 4.0 or 4.1.

## 0.4.0

This is the final release of the plugin that's compatible with **Android Gradle Plugin 4.0 and 4.1**.

Starting from the next release, the latest version of AGP (currently `4.2.0-alpha13`) will be required, until the new variant APIs become stable which is expected to happen with the next major version of AGP.

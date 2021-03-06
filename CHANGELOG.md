# Change Log

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

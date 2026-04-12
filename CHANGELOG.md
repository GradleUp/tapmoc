
# 0.4.2
_2026-04-12_

Version 0.4.2 adds support for getting the Kotlin version for Gradle 5 and improves KDoc. Many thanks @sschuberth for the feedback and contributions in this release.

## What's Changed
* [NEW] Support getting the Kotlin version for Gradle 5 (#93)
* [UPDATE] Update nmcp, tapmoc, and librarian versions used to build tapmoc (no runtime impact) (#85, #86, #87)
* [INFRA] Improve the KDoc of `tapmoc.kotlin()` (#94)
* [INFRA] Simplify parsing the Gradle major version (#91)

# 0.4.1
_2026-03-30_

Couple of bugfixes:

* [FIX] Fix checking dependencies on Android projects (https://github.com/GradleUp/tapmoc/pull/83)
  * Android projects expose several variants, and we need to set the variant and artifact type attributes to avoid disambiguation errors when resolving the runtime/api dependencies.
* [FIX] Fix the kotlin-stdlib exposed by `tapmoc-gradle-plugin` ([faf78a25](https://github.com/GradleUp/tapmoc/commit/faf78a255ccac5c09c671613b60c0d9d8173c904), [a0489f6b](https://github.com/GradleUp/tapmoc/commit/a0489f6ba6d6e6013d4d9f227f80ee685a86d05f), ...)
  * 0.4.0 added new compilations that pulled `kotlin-stdlib:2.3.0` automatically. 0.4.1 now explicitely add the appropriate version of `kotlin-stdlib` to all compilations.

# 0.4.0
_2025-12-29_

New `gradle(String)` helper. New, more granular, way to enable/disable the dependencies checks. As well as a couple of important fixes for Android.   

## Add `TapmocExtension.gradle(String)` (#35)

`TapmocExtension.gradle(String)` makes it easy to configure compatibility for your Gradle plugins:

```kotlin
tapmoc {
  /**
   * Sets Java and Kotlin flags according to
   * https://docs.gradle.org/current/userguide/compatibility.html
   * 
   * This is equivalent to calling `java(8)` and `kotlin("1.8.0")`
   */
  gradle("8.14")
}
```

## `checkDependencies()` does not check the `kotlin-stdlib` version by default anymore (#74).

`kotlin-stdlib` can be safely upgraded in most cases (Gradle plugins is the exception). Calling `checkDependencies()` does not check for mismatched `kotlin-stdlib` versions anymore.

## add `checkJavaClassFiles()`, `checkKotlinMetadata()` and `checkKotlinStdlibs()` (#74).

You may now enable/disable check individually. `checkDependencies()` is still present and calls both `checkJavaClassFiles()`and `checkKotlinMetadata()`.

## All changes

* [NEW] Add `TapmocExtension.gradle()` for configuring Gradle plugins (#35)
* [NEW] Checking for `kotlin-stdlib` versions in dependencies is now a separate check (#74)
* [NEW] Fail if the requested Kotlin version is higher than the KGP version (#72)
* [UPDATE] Update Gratatouille (#75)
* [FIX] Fix getting the java-api and java-runtime for Android (#74)
* [FIX] Fix downgrading the `kotlin-stdlib` version for Android (#74)

# 0.3.2
_2025-12-16_

* [NEW] Ignore class files in `org/gradle/internal/impldep/META-INF/versions/` (#68)
* [FIX] Fix parsing the java version for Java < 5 (#68)
* [FIX] Fix downgrading the JVM kotlin-stdlib for KMP projects (#67, #69)

# 0.3.1
_2025-12-09_

* [NEW] Add a specific error message to warn about `java-gradle-plugin` adding `gradleApi()` in the wrong configuration (#65)
* [FIX] Support for `com.android.test` and `com.android.dynamic-feature` plugins (#62, #63)
* [BREAKING] Remove `com.gradleup.compat.patrouille` legacy plugin. It was only provided as a helper for the rename (#64)

Many thanks @simonlebras for catching the `com.android.test` regression 🙏

# 0.3.0
_2025-12-08_

* [NEW] Add a specific error message to warn about `java-gradle-plugin` adding `gradleApi()` in the wrong configuration (#65)
* [FIX] Support for `com.android.test` and `com.android.dynamic-feature` plugins (#62, #63)
* [BREAKING] Remove `com.gradleup.compat.patrouille` legacy plugin. It was only provided as a helper for the rename (#64)

# 0.2.0
_2025-11-27_

## Project is renamed to `tapmoc`

`tapmoc` is backwards `compat`! Many thanks @JakeWharton for the nice name 💙

You'll need to update your plugin id and extension block:

```kotlin
plugins {
  // Replace
  id("com.gradleup.compat.patrouille").version("0.1.0")
  // With 
  id("com.gradleup.tapmoc").version("0.2.0")
}

// replace 
compatPatrouille {
  java(17)
  kotlin("2.0.0")
}

// with 
tapmoc {
  java(17)
  kotlin("2.0.0")
}
```

## Other changes

* `TapmocExtension::kotlin()` may now be called even if KGP is not present in the build classpath (#42). This makes it easier to use tapmoc in a central convention plugin. It also allows checking runtime dependencies for incompatible usages of `kotlin-stdlib` for Java projects that may rely on Kotlin dependencies.
* Use `implementation` instead of `api` for the `kotlin-stdlib` configuration of non-JVM tests, fixes a warning when using KGP 2.3.0. (#41)
* Make the plugin uses lazier Gradle APIs (#33, #34), many thanks @simonlebras.

# 0.1.0
_2025-10-10_
Add support for `com.android.kotlin.multiplatform.library` in https://github.com/GradleUp/compat-patrouille/pull/31

# 0.0.3
_2025-10-06_

Do not configure `JavaCompile` tasks eagerly (https://github.com/GradleUp/compat-patrouille/issues/27)

# 0.0.3
_2025-10-06_

Do not configure `JavaCompile` tasks eagerly (https://github.com/GradleUp/compat-patrouille/issues/27)

# 0.0.2
_2025-08-20_

A few bugfixes, upgrades and ergonomics improvements. Many thanks @OliverO2 and @Mr3zee for their feedback in this release.

* [NEW] Add compatPatrouilleCheckRuntimeDependencies as a lifecycle task https://github.com/GradleUp/compat-patrouille/pull/23
* [NEW] For JS and Wasm, add the KGP kotlin-stdlib instead of relying on `coreLibrariesVersion` https://github.com/GradleUp/compat-patrouille/pull/24
* [FIX] Make checkApiDependencies lazier https://github.com/GradleUp/compat-patrouille/pull/20
* [FIX] Fix KMP with multiple targets https://github.com/GradleUp/compat-patrouille/pull/21
* [UPGRADE] Use latest kotlin-metadata lib https://github.com/GradleUp/compat-patrouille/pull/22

# 0.0.1
_2025-08-11_

Version `0.0.1` adds two new tasks to check the API and Runtime dependencies and fixes declaring the Kotlin compatibility of common source sets.

* [NEW] Introduce `checkApiDependencies()` by @martinbonnin in https://github.com/GradleUp/compat-patrouille/pull/2
* [NEW] Introduce `checkRuntimeDependencies()` by @martinbonnin in https://github.com/GradleUp/compat-patrouille/pull/16
* [FIX] Fix detecting apiVersion in `commonMain` and `commonTest` source sets by @martinbonnin in https://github.com/GradleUp/compat-patrouille/pull/7
* [UPDATE] update KGP, simplify GitHub actions files by @martinbonnin in https://github.com/GradleUp/compat-patrouille/pull/9
* [UPDATE] Bump gradle to 9 by @martinbonnin in https://github.com/GradleUp/compat-patrouille/pull/15
* [MISC] Add more integration tests by @martinbonnin in https://github.com/GradleUp/compat-patrouille/pull/10

# 0.0.0
_2025-04-08_

Initial release 🎉

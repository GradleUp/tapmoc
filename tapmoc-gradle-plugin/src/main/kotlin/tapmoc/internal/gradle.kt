package tapmoc.internal

internal fun kotlinVersionForGradle(major: Int): String {
  /**
   * We use `languageVersion` as the compatible Kotlin version.
   *
   * Note: `languageVersion` is lower than the Kotlin embedded version.
   * - Gradle may bump the Kotlin embedded version in Gradle minor releases.
   * - The Kotlin `languageVersion` is only bumped in major releases.
   *
   * This is to avoid the occasional Kotlin source-breaking changes to break existing
   * builds in a minor Gradle release.
   *
   * See https://docs.gradle.org/current/userguide/compatibility.html#kotlin
   *
   */
  return when {
    major >= 9 -> "2.2.0"
    major >= 8 -> "1.8.0"
    major >= 7 -> "1.4.0"
    major >= 5 -> "1.3.0"
    else -> error("Gradle versions < 5.0 are not supported (found '$major')")
  }
}

internal fun javaVersionForGradle(major: Int): Int {
  /**
   * See https://docs.gradle.org/current/userguide/compatibility.html#java
   */
  return when {
    major >= 9 -> 17
    major >= 5 -> 8
    else -> error("Gradle versions < 5 are not supported (found '$major')")
  }
}

internal fun parseGradleMajorVersion(gradleVersion: String): Int {
  val c = gradleVersion.split('.')
  val d = c.firstOrNull()

  checkNotNull(d) {
    "Tapmoc: gradleVersion should be in the form `major[.minor[.patch]]` (found '$gradleVersion')"
  }

  val major = d.toIntOrNull()

  checkNotNull(major) {
    "Tapmoc: major version must be an integer (found '$major' in '$gradleVersion')"
  }

  return major
}

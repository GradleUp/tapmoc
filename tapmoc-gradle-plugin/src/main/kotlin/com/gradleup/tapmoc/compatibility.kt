package com.gradleup.tapmoc

import com.gradleup.tapmoc.internal.onAgp
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import com.gradleup.tapmoc.internal.onKgp

fun Project.configureJavaCompatibility(
  javaVersion: Int,
) {
  /**
   * Set --release for all JavaCompile tasks except the ones owned by AGP (if any).
   */
  tasks.withType(JavaCompile::class.java).configureEach {
    if (!isAndroidJavaCompileTask(it.name)) {
      it.options.release.set(javaVersion)
    }
  }

  /**
   * Set the source and target compatibility for AGP (if any).
   */
  onAgp {
    it.javaCompatibility(javaVersion.toJavaVersion())
  }

  onKgp {
    it.javaCompatibility(javaVersion)
  }
}


fun Project.configureKotlinCompatibility(
  version: String,
) {
  require(version.split(".").size == 3) {
    "Tapmoc: cannot parse Kotlin version '$version'. Expected format is X.Y.Z."
  }

  onKgp {
    val kgpVersion = it.version(this)
    // We're using lexicographic comparison here
    if (version > kgpVersion) {
      error("Tapmoc: cannot set compatibility version '$version' because it is higher than the Kotlin Gradle Plugin version '$kgpVersion'")
    }
    it.kotlinCompatibility(version)
  }
}

internal fun Int.toJavaVersion(): JavaVersion {
  return JavaVersion.forClassVersion(this + 44)
}

private fun isAndroidJavaCompileTask(name: String): Boolean {
  /**
   * TODO: this isn't the most typesafe but not sure how to do better
   *
   * See https://cs.android.com/android-studio/platform/tools/base/+/da94db5fa35cdf1d97a02e705bf99fa4bf4676d4:build-system/gradle-core/src/main/java/com/android/build/gradle/tasks/JavaCompile.kt;l=67
   */
  return Regex("compile.*JavaWithJavac").matches(name)
}

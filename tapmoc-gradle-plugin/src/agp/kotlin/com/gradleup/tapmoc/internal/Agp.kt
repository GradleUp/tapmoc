package com.gradleup.tapmoc.internal

import org.gradle.api.JavaVersion

internal interface Agp {
  fun javaCompatibility(javaVersion: JavaVersion)
}

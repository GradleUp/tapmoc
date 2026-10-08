package com.gradleup.tapmoc.internal

import org.gradle.api.Project

internal interface Kgp {
  fun javaCompatibility(version: Int)
  fun kotlinCompatibility(version: String)
  fun version(project: Project): String
}

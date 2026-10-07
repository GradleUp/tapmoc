pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      google()
      maven("https://storage.googleapis.com/gradleup/m2") {
        content {
          includeModule("com.gradleup.gratatouille", "gratatouille-processor")
          includeModule("com.gradleup.nmcp", "nmcp-tasks")
          includeModule("com.gradleup.tapmoc", "tapmoc-tasks")
        }
      }
    }
  }
  repositories {
    maven("https://storage.googleapis.com/gradleup/m2") {
      content {
        includeGroupByRegex("com\\.gradleup\\..*")
      }
    }
    exclusiveContent {
      forRepository { gradlePluginPortal() }
      filter {
        includeModule("org.gradle.toolchains.foojay-resolver-convention", "org.gradle.toolchains.foojay-resolver-convention.gradle.plugin")
        includeModule("org.gradle.toolchains", "foojay-resolver")
      }
    }
  }
}

plugins {
  // Auto-provisions the JDK toolchains
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":tapmoc-gradle-plugin")
include(":tapmoc-tasks")

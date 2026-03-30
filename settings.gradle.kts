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
  }
}

include(":tapmoc-gradle-plugin")
include(":tapmoc-tasks")

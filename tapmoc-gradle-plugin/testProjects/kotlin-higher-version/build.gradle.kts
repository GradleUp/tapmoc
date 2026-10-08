import com.gradleup.tapmoc.Severity

buildscript {
  dependencies {
    classpath("com.gradleup.tapmoc:tapmoc-gradle-plugin:PLACEHOLDER")
  }
}

plugins {
  id("org.jetbrains.kotlin.jvm").version("2.2.0")
}

pluginManager.apply("com.gradleup.tapmoc")

extensions.getByType(com.gradleup.tapmoc.TapmocExtension::class.java).apply {
  java(8)
  kotlin("2.3.0") // This should make an error
}

import com.gradleup.tapmoc.Severity

buildscript {
  dependencies {
    classpath("com.gradleup.tapmoc:tapmoc-gradle-plugin:PLACEHOLDER")
  }
}

plugins {
  alias(libs.plugins.kgp.jvm)
}

pluginManager.apply("com.gradleup.tapmoc")
extensions.getByType(com.gradleup.tapmoc.TapmocExtension::class.java).apply {
  java(11)
  kotlin("1.9.0")
  checkDependencies(Severity.WARNING)
  checkKotlinStdlibs(Severity.WARNING)
}

dependencies {
  // makes `tapmocCheckKotlinMetadata` fail because incompatible metadata was exposed to consumers.
  api("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")
  // makes `tapmocCheckKotlinStdlibVersions` fail because kotlin-stdlib was upgraded to a new version.
  implementation("com.squareup:kotlinpoet:2.2.0")
  api(libs.kotlin.metadata)
}

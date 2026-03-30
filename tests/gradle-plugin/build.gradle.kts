import tapmoc.Severity

plugins {
  id("com.gradleup.tapmoc")
  // Use a version of KGP that can target 1.8
  id("org.jetbrains.kotlin.jvm").version("2.2.10")
  id("java-gradle-plugin") // needs to be before check.publication
  id("check.publication")
}

tapmoc {
  // Gradle 8.3 uses Kotlin languageVersion 1.8
  gradle("8.3")
  checkDependencies(Severity.ERROR)
  checkKotlinStdlibs(Severity.ERROR)
}

checkPublication {
  jvmTarget.set(8)
  kotlinMetadataVersion.set("1.8.0")
}

dependencies {
  compileOnlyApi("dev.gradleplugins:gradle-api:8.3")
}

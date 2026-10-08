import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.gradleup.tapmoc.Severity

plugins {
  alias(libs.plugins.agp8)
  id("com.gradleup.tapmoc")
  id("check.publication")
}

val myJvmTarget = 11
tapmoc {
  java(myJvmTarget)
  checkDependencies(Severity.ERROR)
  checkKotlinStdlibs(Severity.ERROR)
}

android {
  defaultConfig {
    namespace = "com.example"
    minSdk = libs.versions.compile.sdk.get().toInt()
    compileSdk = libs.versions.compile.sdk.get().toInt()
  }


  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
}

checkPublication {
  jvmTarget.set(myJvmTarget)
}

java.sourceSets.create("foo")

// Not sure why this is not automatically setup
tasks.named("build").dependsOn("compileFooJava")

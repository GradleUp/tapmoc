import com.gradleup.librarian.gradle.Librarian
import tapmoc.configureKotlinCompatibility
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.gradleup.gratatouille.tasks")
  id("com.google.devtools.ksp")
}

Librarian.module(project)

dependencies {
  implementation(libs.kotlin.metadata)
  implementation(libs.asm)
  implementation(libs.gratatouille.tasks.runtime)
  testImplementation(kotlin("test"))
}

// Override the default from Librarian, we want to be able to use the latest Kotlin version here.
configureKotlinCompatibility(getKotlinPluginVersion())

gratatouille {
  addDependencies = false
}

extensions.getByType<PublishingExtension>().repositories {
  maven {
    name = "local"
    url = rootDir.resolve("build/m2").toURI()
  }
}

import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille")
}

Librarian.module(project)

kotlin {
  compilerOptions {
    // "Language version 2.0 is deprecated and its support will be removed in a future version of Kotlin"
    freeCompilerArgs.add("-Xsuppress-version-warnings")
  }
}

val optionalPlugins = mapOf(
  "agp" to libs.agp,
  "kgp" to libs.kgp.compile.only,
)

val mainCompilation = kotlin.target.compilations.getByName("main")

optionalPlugins.forEach { (name, dependencyProvider) ->
  val compilation = kotlin.target.compilations.create(name)
  dependencies {
    add(compilation.compileOnlyConfigurationName, dependencyProvider)
    add(compilation.compileOnlyConfigurationName, libs.gradle.api)
  }

  mainCompilation.associateWith(compilation)
  tasks.jar {
    from(compilation.output.classesDirs)
  }

  /**
   * associateWith() pulls the secondary compilations into the main dependencies,
   * which we don't want.
   *
   * An alternative would be to not use `associateWith()` but that fails in the IDE,
   * probably because there is no way to set `AbstractKotlinCompile.friendSourceSets`
   * from public API.
   */
  val dep = dependencyProvider.get()
  configurations.compileOnly.configure {
    dependencies.removeIf {
      when {
        it is ExternalDependency && it.group == dep.group && it.name == dep.name -> true
        else -> false
      }
    }
  }
}

dependencies {
  compileOnly(libs.gradle.api)
  implementation(libs.gratatouille.runtime)
  gratatouille(project(":tapmoc-tasks"))

  testImplementation(gradleTestKit())
  testImplementation(kotlin("test"))
}

gratatouille {
  addDependencies = false
  // for included builds
  pluginLocalPublication("com.gradleup.tapmoc")
  // for publishToMavenLocal
  pluginMarker("com.gradleup.tapmoc")
}

/**
 * Gradle 8.0 does not run on recent JDKs (it supports up to Java 19), so its tests live in a separate
 * source set that runs on a Java 17 toolchain.
 */
val gradle8TestCompilation = kotlin.target.compilations.create("gradle8Test") {
  associateWith(kotlin.target.compilations.getByName("test"))
}

dependencies {
  add(gradle8TestCompilation.implementationConfigurationName, gradleTestKit())
  add(gradle8TestCompilation.implementationConfigurationName, kotlin("test"))
}

val gradle8Test = tasks.register<Test>("gradle8Test") {
  group = "verification"
  testClassesDirs = gradle8TestCompilation.output.classesDirs
  classpath = gradle8TestCompilation.output.allOutputs +
    gradle8TestCompilation.runtimeDependencyFiles +
    kotlin.target.compilations.getByName("test").output.allOutputs
  javaLauncher.set(javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(17))
  })
}

tasks.named("check") {
  dependsOn(gradle8Test)
}

val cleanTestProjects = tasks.register<Delete>("cleanTestProjects") {
  description = "Deletes the test projects left over by previous (failed) test runs."
  delete(layout.buildDirectory.map { buildDir ->
    buildDir.asFile.listFiles().orEmpty().filter { it.name.startsWith("testProject-") }
  })
}

tasks.withType<Test>().configureEach {
  dependsOn(cleanTestProjects)
  dependsOn("publishAllPublicationsToLocalRepository")
  dependsOn(":tapmoc-tasks:publishAllPublicationsToLocalRepository")
}

extensions.getByType<PublishingExtension>().repositories {
  maven {
    name = "local"
    url = rootDir.resolve("build/m2").toURI()
  }
}

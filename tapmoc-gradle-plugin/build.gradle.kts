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
  pluginLocalPublication("com.gradleup.tapmoc")
}

tasks.withType<Test>().configureEach {
  dependsOn("publishAllPublicationsToLocalRepository")
  dependsOn(":tapmoc-tasks:publishAllPublicationsToLocalRepository")
}

extensions.getByType<PublishingExtension>().repositories {
  maven {
    name = "local"
    url = rootDir.resolve("build/m2").toURI()
  }
}

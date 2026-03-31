import com.gradleup.librarian.gradle.Librarian

buildscript {
  this.configurations.configureEach {
    resolutionStrategy {
      eachDependency {
        if (this.requested.name == "dokka-gradle-plugin") {
          // 
          useVersion("2.1.0")
        }
      }
    }
  }
}

plugins {
  alias(libs.plugins.kgp.jvm).apply(false)
  alias(libs.plugins.librarian).apply(false)
  alias(libs.plugins.nmcp).apply(false)
  alias(libs.plugins.tapmoc).apply(false)
  alias(libs.plugins.gratatouille).apply(false)
  alias(libs.plugins.ksp).apply(false)
}

Librarian.root(project)

tasks.register("docsNpmInstall", Exec::class.java) {
  enabled = file("docs").exists()

  commandLine("npm", "ci")
  workingDir("docs")
}

tasks.register("docsNpmBuild", Exec::class.java) {
  dependsOn("docsNpmInstall")

  enabled = file("docs").exists()

  commandLine("npm", "run", "build")
  workingDir("docs")
}

tasks.named("librarianStaticContent").configure {
  dependsOn("docsNpmBuild")

  val from = file("docs/dist")
  doLast {
    from.copyRecursively(outputs.files.single(), overwrite = true)
  }
}

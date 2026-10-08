import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider
import com.gradleup.tapmoc.Severity

buildscript {
  dependencies {
    classpath("com.gradleup.tapmoc:tapmoc-gradle-plugin:PLACEHOLDER")
  }
}

plugins {
  id("java")
}

pluginManager.apply("com.gradleup.tapmoc")
extensions.getByType(com.gradleup.tapmoc.TapmocExtension::class.java).apply {
  java(8)
  kotlin("2.0.0") // This should be a no-op
  checkDependencies(Severity.ERROR)
}

abstract class GenerateClasses: DefaultTask() {
  @get:OutputDirectory
  abstract val output: DirectoryProperty

  @get:InputFile
  abstract val source: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val javac = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    javac.getStandardFileManager(diagnostics, null, null).use { fm ->
      fm.setLocation(StandardLocation.CLASS_OUTPUT, listOf(output.get().asFile))

      val javaSourceFiles = listOf(source.asFile.get())
      val fileObjects = fm.getJavaFileObjectsFromFiles(javaSourceFiles)

      val options = buildList {
        add("--release")
        add("11") // Compile using a higher version
      }

      val task = javac.getTask(null, fm, diagnostics, options, null, fileObjects)

      val success = task.call()
      if (!success) {
        error("Java compilation failed.")
      }
    }
  }
}

val generateClasses = tasks.register("javaClasses", GenerateClasses::class.java) {
  output.set(file("build/javaClasses"))
  source.set(file("Hello.java"))
}

val jar = tasks.register("javaJar", Jar::class.java) {
  from(generateClasses)
  archiveClassifier = "higher"
}

dependencies {
  implementation(files(jar))
}

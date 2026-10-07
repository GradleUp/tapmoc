import java.io.File
import java.util.Properties
import java.util.UUID
import org.gradle.testkit.runner.GradleRunner

fun gradleRunner(dir: File, vararg args: String): GradleRunner {
  return GradleRunner.create()
    .withProjectDir(dir)
    .withDebug(false)
    .withArguments(*args)
    .forwardOutput()
}

fun withTestProject(name: String, block: (File) -> Unit) {
  val src = File("testProjects/$name")
  // Must stay directly under "build/": the test projects reference "../../../build/m2".
  // The directory is unique so that tests can run in parallel.
  val dst = File("build/testProject-$name-${UUID.randomUUID()}")

  src.copyRecursively(dst)

  dst.walk().onLeave {
    if (it.isDirectory && it.name == "build") {
      it.deleteRecursively()
    }
  }.count() // count is just used to collect the sequence

  val currentVersion = Properties().apply {
    File("../librarian.root.properties").reader().use {
      load(it)
    }
  }
  listOf("build.gradle.kts", "build.gradle").map { dst.resolve(it) }.first { it.exists() }.let {
    it.writeText(it.readText().replace("PLACEHOLDER", currentVersion.get("pom.version").toString()))
  }
  block(dst)
  // Keep the directory around if the test fails so it can be inspected
  dst.deleteRecursively()
}

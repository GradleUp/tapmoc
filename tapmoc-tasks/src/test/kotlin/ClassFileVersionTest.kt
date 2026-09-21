import gratatouille.tasks.FileWithPath
import gratatouille.tasks.GLogger
import org.junit.Test
import tapmoc.task.tapmocCheckClassFileVersions
import java.io.File
import java.net.URI
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal object SystemGLogger : GLogger {
  override fun debug(message: String) {
    println("D: $message")
  }

  override fun info(message: String) {
    println("I: $message")
  }

  override fun lifecycle(message: String) {
    println("L: $message")
  }

  override fun warn(message: String) {
    println("W: $message")
  }

  override fun error(message: String) {
    println("E: $message")
  }
}

class ClassFileVersionTest {
  @Test
  fun testMultiReleaseJarDoesntFail() {
    val jarFile = downloadFromMavenCentral("org/junit/platform/junit-platform-commons/1.14.4/junit-platform-commons-1.14.4.jar")
    val output = File.createTempFile("checkClassFileVersions", ".txt")
    output.deleteOnExit()

    tapmocCheckClassFileVersions(
      logger = SystemGLogger,
      warningAsError = true,
      jarFiles = listOf(FileWithPath(jarFile, jarFile.name)),
      javaVersion = 8,
      output = output,
    )
  }

  @Test
  fun testNewerJarFails() {
    val jarFile = downloadFromMavenCentral("org/http4k/http4k-core/6.60.0.0/http4k-core-6.60.0.0.jar")
    val output = File.createTempFile("checkClassFileVersions", ".txt")
    output.deleteOnExit()

    val exception = assertFailsWith<IllegalStateException> {
      tapmocCheckClassFileVersions(
        logger = SystemGLogger,
        warningAsError = true,
        jarFiles = listOf(FileWithPath(jarFile, jarFile.name)),
        javaVersion = 8,
        output = output,
      )
    }

    assertTrue(exception.message!!.contains("targets class file version 65.0 (Java 21) which is newer than supported <= 52 (Java 8)"))
  }

  private fun downloadFromMavenCentral(path: String): File {
    val cacheFile = File(System.getProperty("java.io.tmpdir")).resolve("tapmoc-tests").resolve(path)
    if (!cacheFile.exists()) {
      cacheFile.parentFile.mkdirs()
      val url =
        URI("https://repo1.maven.org/maven2/$path").toURL()
      url.openStream().use { input ->
        cacheFile.outputStream().use { output -> input.copyTo(output) }
      }
    }
    return cacheFile
  }
}

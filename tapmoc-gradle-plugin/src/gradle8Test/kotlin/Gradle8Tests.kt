import kotlin.test.Test
import org.junit.Assert.assertTrue

class Gradle8Tests {
  @Test
  fun dependencyMismatchIsDetectedWithGradle8_0() {
    withTestProject("gradle-8-0") {
      gradleRunner(it, "build").withGradleVersion("8.0").buildAndFail().apply {
        println(output.toString())
        assertTrue(output.contains("targets class file version 55.0 (Java 11) which is newer than supported <= 52 (Java 8)."))
      }
    }
  }
}

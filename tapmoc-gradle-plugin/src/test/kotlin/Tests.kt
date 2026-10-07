import kotlin.test.Test
import org.junit.Assert.assertTrue

class Tests {
  @Test
  fun wrongJavaBytecodeIsDetected() {
    withTestProject("java") {
      gradleRunner(it, "build").buildAndFail().apply {
        assertTrue(output.contains("targets class file version 55.0 (Java 11) which is newer than supported <= 52 (Java 8)."))
      }
    }
  }

  @Test
  fun metaInfIsExcluded() {
    withTestProject("java-meta-inf") {
      gradleRunner(it, "build").build()
    }
  }

  @Test
  fun checkDependenciesDisplaysWarnings() {
    withTestProject("check-dependencies") {
      gradleRunner(it, "build", "--continue").build().apply {
        assertTrue(output.contains("contains unsupported metadata"))
        assertTrue(output.contains("incompatible kotlin-stdlib"))
      }
    }
  }

  @Test
  fun kotlinHigherThanKGPFails() {
    withTestProject("kotlin-higher-version") {
      gradleRunner(it, "build").buildAndFail().apply {
        assertTrue(output.contains("Tapmoc: cannot set compatibility version '2.3.0' because it is higher than the Kotlin Gradle Plugin version '2.2.0'"))
      }
    }
  }
}

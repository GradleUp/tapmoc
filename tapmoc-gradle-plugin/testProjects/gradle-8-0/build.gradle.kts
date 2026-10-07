import tapmoc.Severity

plugins {
  id("java")
  id("com.gradleup.tapmoc").version("PLACEHOLDER")
}

extensions.getByType(tapmoc.TapmocExtension::class.java).apply {
  java(8)
  checkDependencies(Severity.ERROR)
}

dependencies {
  // jetty 11 targets Java 11 (class file version 55)
  implementation("org.eclipse.jetty:jetty-util:11.0.20")
}

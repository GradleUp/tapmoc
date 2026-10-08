package tapmoc

import org.gradle.api.Project
import com.gradleup.tapmoc.configureJavaCompatibility as newConfigureJavaCompatibility
import com.gradleup.tapmoc.configureKotlinCompatibility as newConfigureKotlinCompatibility

@Deprecated(
  "Use com.gradleup.tapmoc.configureJavaCompatibility instead.",
  ReplaceWith("configureJavaCompatibility(javaVersion)", "com.gradleup.tapmoc.configureJavaCompatibility"),
  level = DeprecationLevel.ERROR
)
fun Project.configureJavaCompatibility(javaVersion: Int) {
  newConfigureJavaCompatibility(javaVersion)
}

@Deprecated(
  "Use com.gradleup.tapmoc.configureKotlinCompatibility instead.",
  ReplaceWith("configureKotlinCompatibility(version)", "com.gradleup.tapmoc.configureKotlinCompatibility"),
  level = DeprecationLevel.ERROR
)
fun Project.configureKotlinCompatibility(version: String) {
  newConfigureKotlinCompatibility(version)
}

package tapmoc

import org.gradle.api.provider.Provider

interface TapmocExtension {
  /**
   * Configures the version of Java to target.
   * This version is used as:
   * - targetCompatibility
   * - sourceCompatibility
   * - release (if not on android)
   *
   * @param version the version of Java to target.
   * Examples: 8, 11, 17, 21, 24, ...
   */
  fun java(version: Int)

  /**
   * Configures the version of Kotlin to target.
   * This version is used as:
   * - kotlin-stdlib JVM version
   * - languageVersion
   * - apiVersion
   *
   * @param version the version of Kotlin to target.
   * languageVersion and apiVersion only use the `major.minor` (e.g "1.9") of [version].
   * `kotlin-stdlib` JVM version uses [version] verbatim.
   *
   * Examples: "1.9.0", "1.9.22", "2.0.21", "2.1.20", ...
   */
  fun kotlin(version: String)

  /**
   * Configures compatibility flags for the minimal Gradle version supported.
   *
   * This method:
   * - Calls [kotlin] with the compatible Kotlin version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#kotlin).
   * - Calls [java] with the compatible Java version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#java_runtime).
   *
   * It is equivalent to the following code:
   * ```kotlin
   * kotlin(kotlinVersionForGradle(gradleVersion))
   * java(javaVersionForGradle(gradleVersion))
   * ```
   *
   * Note: When building a Gradle plugin, calling `checkDependencies(Severity.ERROR)` and `checkKotlinStdlibs(Severity.ERROR)` is strongly recommended.
   *
   * @param gradleVersion the Gradle version to target, specified as a string. Example: "8.14".
   *
   * @see checkDependencies
   * @see checkKotlinStdlibs
   */
  fun gradle(gradleVersion: String)

  /**
   * Returns the minimal version of Java required to run the given Gradle version.
   *
   * Gradle versions between 2.0 and 8.14 require Java 8.
   * Gradle 9.0.0 requires Java 17.
   *
   * See https://docs.gradle.org/current/userguide/compatibility.html#java
   */
  fun javaVersionForGradle(gradleVersion: String): Int

  /**
   * Returns the languageVersion used to compile Kotlin build scripts.
   *
   * See https://docs.gradle.org/current/userguide/compatibility.html#kotlin
   */
  fun kotlinVersionForGradle(gradleVersion: String): String

  /**
   * Registers a `tapmoc${Configuration}` resolvable configuration extending from [configuration] and a `tapmocCheck${Configuration}JavaClassFiles` task.
   * `tapmocCheck${Configuration}JavaClassFiles` scans all class files and checks that the class file version is compatible with the declared Java version.
   *
   * @param configuration the name of the configuration to check.
   * @param severity The severity level for the check.
   */
  fun checkJavaClassFiles(configuration: String, severity: Provider<Severity>)

  /**
   * For each "known" api and runtime configuration, registers a `tapmoc${Configuration}` resolvable configuration extending from the configuration and a `tapmocCheck${Configuration}JavaClassFiles` task.
   * `tapmocCheck${Configuration}JavaClassFiles` scans all class files and checks that the class file version is compatible with the declared Java version.
   *
   * The "known" configurations are detected based on known names ("apiElements", "runtimeElements", "jvmRuntimeElements", etc...). For more advanced use cases, specify the configuration name explicitly.
   *
   * @param severity The severity level for the check.
   */
  fun checkJavaClassFiles(severity: Provider<Severity>)

  /**
   * Same as [checkJavaClassFiles] with a constant [severity].
   *
   * @param severity The severity level for the check.
   * @see checkJavaClassFiles
   */
  fun checkJavaClassFiles(severity: Severity)

  /**
   * Registers a `tapmoc${Configuration}` resolvable configuration extending from [configuration] and a `tapmocCheck${Configuration}KotlinMetadata` task.
   * `tapmocCheck${Configuration}KotlinMetadata` scans all `.kotlin_module` files and checks that the Kotlin metadata version is compatible with the declared Kotlin version.
   *
   * @param configuration the name of the configuration to check.
   * @param severity The severity level for the check.
   */
  fun checkKotlinMetadata(configuration: String, severity: Provider<Severity>)

  /**
   * For each "known" api configuration, registers a `tapmoc${Configuration}` resolvable configuration extending from the configuration and a `tapmocCheck${Configuration}KotlinMetadata` task.
   * `tapmocCheck${Configuration}KotlinMetadata` scans all `.kotlin_module` files and checks that the Kotlin metadata version is compatible with the declared Kotlin version.
   *
   * The "known" configurations are detected based on known names ("apiElements", "jvmApiElements", etc...). For more advanced use cases, specify the configuration name explicitly.
   *
   * @param severity The severity level for the check.
   */
  fun checkKotlinMetadata(severity: Provider<Severity>)

  /**
   * Same as [checkKotlinMetadata] with a constant [severity].
   *
   * @param severity The severity level for the check.
   * @see checkKotlinMetadata
   */
  fun checkKotlinMetadata(severity: Severity)

  /**
   * Registers a `tapmoc${Configuration}` resolvable configuration extending from [configuration] and a `tapmocCheck${Configuration}KotlinStdlib` task.
   * `tapmocCheck${Configuration}KotlinStdlib` checks that the resolved `kotlin-stdlib` versions are not higher than the declared Kotlin version.
   *
   * In most cases, `kotlin-stdlib` can be safely upgraded, and this check is not enabled by [checkDependencies].
   * Enable it if your runtime forces a given version of `kotlin-stdlib`. This is the case for Gradle plugins in particular.
   *
   * @param configuration the name of the configuration to check.
   * @param severity The severity level for the check.
   */
  fun checkKotlinStdlibs(configuration: String, severity: Provider<Severity>)

  /**
   * For each "known" runtime configuration, registers a `tapmoc${Configuration}` resolvable configuration extending from the configuration and a `tapmocCheck${Configuration}KotlinStdlib` task.
   * `tapmocCheck${Configuration}KotlinStdlib` checks that the resolved `kotlin-stdlib` versions are not higher than the declared Kotlin version.
   *
   * The "known" configurations are detected based on known names ("runtimeElements", "jvmRuntimeElements", etc...). For more advanced use cases, specify the configuration name explicitly.
   *
   * @param severity The severity level for the check.
   */
  fun checkKotlinStdlibs(severity: Provider<Severity>)

  /**
   * Same as [checkKotlinStdlibs] with a constant [severity].
   *
   * @param severity The severity level for the check.
   * @see checkKotlinStdlibs
   */
  fun checkKotlinStdlibs(severity: Severity)

  /**
   * Enables verification of Java class file versions and Kotlin metadata version on the "known" outgoing configurations.
   *
   * What constitutes a "known" configuration is based on usual configuration names. For more advanced use cases, specify the configuration name explicitly.
   *
   * [checkDependencies] is equivalent to calling [checkJavaClassFiles] and [checkKotlinMetadata].
   *
   * [checkDependencies] doesn't call [checkKotlinStdlibs] as kotlin-stdlib is usually upgraded at runtime. One notable exception is Gradle plugins.
   * If you are developing a Gradle plugin, you may want to call [checkKotlinStdlibs] as well.
   *
   * @param severity The severity level for the check.
   * @see checkJavaClassFiles
   * @see checkKotlinMetadata
   */
  fun checkDependencies(severity: Provider<Severity>)

  /**
   * Same as [checkDependencies] with a constant [severity].
   *
   * @param severity The severity level for the check.
   * @see checkDependencies
   */
  fun checkDependencies(severity: Severity)

  /**
   * This is equivalent to calling `checkDependencies(Severity.ERROR)`.
   *
   * @see checkDependencies
   */
  fun checkDependencies()

  @Deprecated("Use checkDependencies instead.", ReplaceWith("checkDependencies(severity)"), level = DeprecationLevel.ERROR)
  fun checkApiDependencies(severity: Severity)

  @Deprecated("Use checkDependencies instead.", ReplaceWith("checkDependencies(severity)"), level = DeprecationLevel.ERROR)
  fun checkRuntimeDependencies(severity: Severity)
}

enum class Severity {
  /**
   * Do not run the check.
   */
  IGNORE,

  /**
   * Log a warning when the check finds an incompatibility.
   */
  WARNING,

  /**
   * Fail the build when the check finds an incompatibility.
   */
  ERROR
}

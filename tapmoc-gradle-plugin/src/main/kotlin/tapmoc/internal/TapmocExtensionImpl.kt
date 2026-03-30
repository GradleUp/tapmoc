package tapmoc.internal

import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import tapmoc.Severity
import tapmoc.TapmocExtension
import tapmoc.configureJavaCompatibility
import tapmoc.configureKotlinCompatibility
import tapmoc.task.registerTapmocCheckClassFileVersionsTask
import tapmoc.task.registerTapmocCheckKotlinMetadataVersionsTask
import tapmoc.task.registerTapmocCheckKotlinStdlibVersionsTask

internal abstract class TapmocExtensionImpl(private val project: Project) : TapmocExtension {
  abstract val javaClassFilesSeverity: Property<Severity>
  abstract val kotlinMetadataSeverity: Property<Severity>
  abstract val kotlinStdlibSeverity: Property<Severity>

  abstract val kotlinVersionProvider: Property<String>
  abstract val javaVersionProvider: Property<Int>

  init {
    javaClassFilesSeverity.convention(Severity.IGNORE)
    kotlinMetadataSeverity.convention(Severity.IGNORE)
    kotlinStdlibSeverity.convention(Severity.IGNORE)

    val apiDependencies = configuration("tapmocApiDependencies", Usage.JAVA_API)
    val runtimeDependencies = configuration("tapmocRuntimeDependencies", Usage.JAVA_RUNTIME)

    val checkJavaClassFiles = project.registerTapmocCheckClassFileVersionsTask(
      warningAsError = javaClassFilesSeverity.map { it == Severity.ERROR },
      javaVersion = javaVersionProvider,
      jarFiles = project.files(apiDependencies, runtimeDependencies)
    )
    checkJavaClassFiles.configure {
      it.enabled = javaClassFilesSeverity.get() != Severity.IGNORE
    }

    val checkKotlinMetadatas = project.registerTapmocCheckKotlinMetadataVersionsTask(
      warningAsError = kotlinMetadataSeverity.map { it == Severity.ERROR },
      kotlinVersion = kotlinVersionProvider,
      files = project.files(apiDependencies),
    )
    checkKotlinMetadatas.configure {
      it.enabled = kotlinMetadataSeverity.get() != Severity.IGNORE
    }

    val checkKotlinStdlibs = project.registerTapmocCheckKotlinStdlibVersionsTask(
      warningAsError = kotlinStdlibSeverity.map { it == Severity.ERROR },
      kotlinVersion = kotlinVersionProvider,
      kotlinStdlibVersions = runtimeDependencies.map {
        it.incoming.resolutionResult.allComponents
          .mapNotNull { (it.id as? ModuleComponentIdentifier) }
          .filter {
            it.group == "org.jetbrains.kotlin" && it.module == "kotlin-stdlib"
          }.map {
            it.version
          }.toSet()
      },
    )
    checkKotlinStdlibs.configure {
      it.enabled = kotlinStdlibSeverity.get() != Severity.IGNORE
    }

    project.plugins.withType(LifecycleBasePlugin::class.java) {
      project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
        it.dependsOn(checkKotlinStdlibs)
        it.dependsOn(checkKotlinMetadatas)
        it.dependsOn(checkJavaClassFiles)
      }
    }
  }

  /**
   * Returns a Provider that configures the underlying configuration the first time
   * it is accessed.
   *
   * We cannot use the regular `.configure{}` because the generated accessors call it too
   * early.
   * We also need it to be lazy because AGP/KGP set their attributes later in the lifecycle.
   *
   * See https://github.com/gradle/gradle/issues/36147
   */
  private fun configuration(name: String, usage: String): Provider<Configuration> {
    val provider = project.configurations.register(name) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.isVisible = false

      it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, usage))
      /**
       * For Android, select "release" by default. This is probably not 100% correct, but fixes errors like those:
       *
       * ```
       * Could not determine the dependencies of task ':openfeedback-m3:tapmocCheckClassFileVersions'.
       * > Could not resolve all dependencies for configuration ':openfeedback-m3:tapmocRuntimeDependencies'.
       *    > Could not resolve project :openfeedback-resources.
       *      Required by:
       *          project :openfeedback-m3
       *       > The consumer was configured to find a component for use during runtime. However we cannot choose between the following variants of project :openfeedback-resources:
       *           - debugRuntimeElements
       *           - releaseRuntimeElements
       *         All of them match the consumer attributes:
       *           - Variant 'debugRuntimeElements' capability 'io.openfeedback:openfeedback-resources:1.0.0-alpha.5-SNAPSHOT' declares a component for use during runtime:
       *               - Unmatched attributes:
       *                   - Provides a library but the consumer didn't ask for it
       *                   - Provides attribute 'com.android.build.api.attributes.AgpVersionAttr' with value '8.9.0' but the consumer didn't ask for it
       *                   - Provides attribute 'com.android.build.api.attributes.BuildTypeAttr' with value 'debug' but the consumer didn't ask for it
       *                   - Provides attribute 'com.android.build.gradle.internal.attributes.VariantAttr' with value 'debug' but the consumer didn't ask for it
       *                   - Provides attribute 'org.gradle.jvm.environment' with value 'android' but the consumer didn't ask for it
       *                   - Provides attribute 'org.jetbrains.kotlin.platform.type' with value 'androidJvm' but the consumer didn't ask for it
       *           - Variant 'releaseRuntimeElements' capability 'io.openfeedback:openfeedback-resources:1.0.0-alpha.5-SNAPSHOT' declares a component for use during runtime:
       *               - Unmatched attributes:
       *                   - Provides a library but the consumer didn't ask for it
       *                   - Provides attribute 'com.android.build.api.attributes.AgpVersionAttr' with value '8.9.0' but the consumer didn't ask for it
       *                   - Provides attribute 'com.android.build.api.attributes.BuildTypeAttr' with value 'release' but the consumer didn't ask for it
       *                   - Provides attribute 'com.android.build.gradle.internal.attributes.VariantAttr' with value 'release' but the consumer didn't ask for it
       *                   - Provides attribute 'org.gradle.jvm.environment' with value 'android' but the consumer didn't ask for it
       *                   - Provides attribute 'org.jetbrains.kotlin.platform.type' with value 'androidJvm' but the consumer didn't ask for it
       * ```
       *
       * I'm expecting the attributes to be ignored in the other cases because "missing attributes are a match" 🤞
       */
      it.attributes.attribute(Attribute.of("com.android.build.gradle.internal.attributes.VariantAttr", String::class.java), "release")
      it.attributes.attribute(Attribute.of("artifactType", String::class.java), "jar")
    }

    var firstTime = true
    return project.provider {
      if (firstTime) {
        project.getConfigurations(usage).forEach {
          provider.get().extendsFrom(it)
        }
        firstTime = false
      }
      provider.get()
    }
  }

  override fun java(version: Int) {
    javaVersionProvider.set(version)
    project.configureJavaCompatibility(version)
  }

  override fun kotlin(version: String) {
    kotlinVersionProvider.set(version)
    project.configureKotlinCompatibility(version)
  }

  override fun gradle(gradleVersion: String) {
    val major = parseGradleMajorVersion(gradleVersion)
    kotlin(kotlinVersionForGradle(major))
    java(javaVersionForGradle(major))
  }

  override fun javaVersionForGradle(gradleVersion: String): Int {
    return javaVersionForGradle(parseGradleMajorVersion(gradleVersion))
  }

  override fun kotlinVersionForGradle(gradleVersion: String): String {
    return kotlinVersionForGradle(parseGradleMajorVersion(gradleVersion))
  }

  override fun checkJavaClassFiles(severity: Severity) {
    javaClassFilesSeverity.set(severity)
  }

  override fun checkKotlinMetadata(severity: Severity) {
    kotlinMetadataSeverity.set(severity)
  }

  override fun checkKotlinStdlibs(severity: Severity) {
    kotlinStdlibSeverity.set(severity)
  }

  override fun checkDependencies() {
    checkDependencies(Severity.ERROR)
  }

  @Suppress("DEPRECATION")
  override fun checkDependencies(severity: Severity) {
    checkJavaClassFiles(severity)
    checkKotlinMetadata(severity)
  }

  @Deprecated(
    "Use checkDependencies instead.",
    replaceWith = ReplaceWith("checkDependencies(severity)"),
    level = DeprecationLevel.ERROR
  )
  override fun checkApiDependencies(severity: Severity) {
    TODO()
  }

  @Deprecated(
    "Use checkDependencies instead.",
    replaceWith = ReplaceWith("checkDependencies(severity)"),
    level = DeprecationLevel.ERROR
  )
  override fun checkRuntimeDependencies(severity: Severity) {
    TODO()
  }
}

/**
 * Retrieves the outgoing configurations for this project.
 *
 * We currently only check the JVM configurations.
 */
private fun Project.getConfigurations(usage: String): NamedDomainObjectSet<Configuration> {
  return configurations.matching {
    it.isCanBeConsumed
        && it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name == usage
        /**
         * releaseSourcesElements declare the `java-runtime` attribute, and we need to set the category to remove it:
         *
         * ```
         * Attributes
         *     - com.android.build.api.attributes.AgpVersionAttr          = 8.12.0
         *     - com.android.build.api.attributes.BuildTypeAttr           = release
         *     - com.android.build.gradle.internal.attributes.VariantAttr = release
         *     - org.gradle.category                                      = documentation
         *     - org.gradle.dependency.bundling                           = external
         *     - org.gradle.docstype                                      = sources
         *     - org.gradle.jvm.environment                               = android
         *     - org.gradle.libraryelements                               = jar
         *     - org.gradle.usage                                         = java-runtime
         *     - org.jetbrains.kotlin.platform.type                       = androidJvm
         * ```
         */
        && it.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name != Category.DOCUMENTATION
  }
}


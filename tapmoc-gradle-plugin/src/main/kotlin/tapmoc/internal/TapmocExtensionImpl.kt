package tapmoc.internal

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DefaultNamedDomainObjectCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import tapmoc.Severity
import tapmoc.TapmocExtension
import tapmoc.configureJavaCompatibility
import tapmoc.configureKotlinCompatibility
import tapmoc.task.registerTapmocCheckClassFileVersionsTask
import tapmoc.task.registerTapmocCheckKotlinMetadataVersionsTask
import tapmoc.task.registerTapmocCheckKotlinStdlibVersionsTask

internal abstract class TapmocExtensionImpl(private val project: Project) : TapmocExtension {
  abstract val kotlinVersionProvider: Property<String>
  abstract val javaVersionProvider: Property<Int>

  private fun addToCheckTask(taskProvider: TaskProvider<*>) {
    project.plugins.withType(LifecycleBasePlugin::class.java) {
      project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
        it.dependsOn(taskProvider)
      }
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

  private fun configurationFor(configuration: String): NamedDomainObjectProvider<Configuration> {
    val name = lowerCameCase("tapmoc", configuration)
    val existing: NamedDomainObjectProvider<Configuration>? = try {
      project.configurations.named(name)
    } catch (_: UnknownDomainObjectException) {
      null
    }
    if (existing != null) {
      return existing
    }
    return project.configurations.register(name) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.isVisible = false
      it.extendsFrom(project.configurations.getByName(configuration))
    }
  }

  private fun fileCollectionFor(configuration: String): FileCollection {
    return project.files(configurationFor(configuration))
  }

  override fun checkJavaClassFiles(configuration: String, severity: Severity) {
    val checkJavaClassFiles = project.registerTapmocCheckClassFileVersionsTask(
      taskName = lowerCameCase("tapmoc", "check", configuration, "JavaClassFiles"),
      warningAsError = project.provider { severity == Severity.ERROR },
      javaVersion = javaVersionProvider,
      jarFiles = project.files(fileCollectionFor(configuration))
    )
    addToCheckTask(checkJavaClassFiles)
  }

  override fun checkJavaClassFiles(severity: Severity) {
    reactToConfigurations(
      onApi = {},
      onRuntime = { checkJavaClassFiles(it, severity)}
    )
  }


  override fun checkKotlinMetadata(configuration: String, severity: Severity) {
    val checkKotlinMetadatas = project.registerTapmocCheckKotlinMetadataVersionsTask(
      taskName = lowerCameCase("tapmoc", "check", configuration, "KotlinMetadata"),
      warningAsError = project.provider { severity == Severity.ERROR },
      kotlinVersion = kotlinVersionProvider,
      files = fileCollectionFor(configuration),
    )
    addToCheckTask(checkKotlinMetadatas)
  }

  override fun checkKotlinMetadata(severity: Severity) {
    reactToConfigurations(
      onApi = {checkKotlinMetadata(it, severity) },
      onRuntime = {}
    )
  }


  override fun checkKotlinStdlibs(configuration: String, severity: Severity) {
    val checkKotlinStdlibs = project.registerTapmocCheckKotlinStdlibVersionsTask(
      taskName = lowerCameCase("tapmoc", "check", configuration, "KotlinStdlib"),
      warningAsError = project.provider { severity == Severity.ERROR },
      kotlinVersion = kotlinVersionProvider,
      kotlinStdlibVersions = configurationFor(configuration).map {
        it.incoming.resolutionResult.allComponents
          .mapNotNull { (it.id as? ModuleComponentIdentifier) }
          .filter {
            it.group == "org.jetbrains.kotlin" && it.module == "kotlin-stdlib"
          }.map {
            it.version
          }.toSet()
      },
    )

    addToCheckTask(checkKotlinStdlibs)
  }

  override fun checkKotlinStdlibs(severity: Severity) {
    reactToConfigurations(
      onApi = { },
      onRuntime = { checkKotlinStdlibs(it, severity) }
    )
  }

  override fun checkDependencies() {
    checkDependencies(Severity.ERROR)
  }

  private fun reactToConfigurations(onApi: (String) -> Unit, onRuntime: (String) -> Unit) {
    val visitedConfigurations = mutableSetOf<String>()

    /**
     * Uses internal APIs. Before that, I tried:
     * - determining the "good" configurations based on their attributes, and it's a mess that creates resolution errors
     * - reacting to plugins being applied but in some cases, the configurations are not always registered from `Plugin::apply`
     *
     * Instead, "react" to known configuration being added. This is all a giant spaghetti plate, but I have no clue how to make
     * things better at this point.
     * If this doesn't work, there is always the explicit API that takes a configuration name.
     *
     * See https://github.com/gradle/gradle/issues/25262
     */
    val onConfiguration = { name: String ->
      if (visitedConfigurations.add(name)) {
        when (name) {
          "apiElements" -> onApi(name)
          "runtimeElements" -> onRuntime(name)
          "jvmApiElements" -> onApi(name)
          "jvmRuntimeElements" -> onRuntime(name)
          "releaseApiElements" -> onApi(name)
          "releaseRuntimeElements" -> onRuntime(name)
          "debugApiElements" -> onApi(name)
          "debugRuntimeElements" -> onRuntime(name)
        }
      }
    }

    // Handle the existing configurations from a snapshot: the callbacks create configurations
    // and Gradle 8.0 throws a ConcurrentModificationException if that happens while whenElementKnown() replays them.
    project.configurations.names.toList().forEach(onConfiguration)

    @Suppress("UNCHECKED_CAST")
    (project.configurations as DefaultNamedDomainObjectCollection<Configuration>).whenElementKnown {
      onConfiguration(it.name)
    }
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

private fun lowerCameCase(vararg components: String): String {
  return components
    .filter { it.isNotEmpty() }
    .mapIndexed { index, component ->
      if (index == 0) {
        component.replaceFirstChar { it.lowercase() }
      } else {
        component.replaceFirstChar { it.uppercase() }
      }
    }
    .joinToString("")
}

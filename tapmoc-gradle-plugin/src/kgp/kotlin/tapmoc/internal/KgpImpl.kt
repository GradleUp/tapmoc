package tapmoc.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

private val compilerOptionsMethod by lazy {
  try {
    KotlinMultiplatformExtension::class.java.getMethod("getCompilerOptions")
  } catch (_: NoSuchMethodException) {
    null
  }
}

private class KgpImpl(private val dependencyHandler: DependencyHandler, extension: Any, private val providers: ProviderFactory, private val kgpVersion: String) : Kgp {
  private val kotlinProjectExtension: KotlinProjectExtension = extension as KotlinProjectExtension

  override fun javaCompatibility(version: Int) {
    kotlinProjectExtension.forEachCompilerOptions { platformType ->
      when (this) {
        is KotlinJvmCompilerOptions -> {
          if (platformType != KotlinPlatformType.androidJvm) {
            /**
             * See https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/tasks/JavaCompileUtils.kt;l=410?q=Using%20%27--release%27%20option%20for%20JavaCompile%20is%20not%20supported%20because%20it%20prevents%20the%20Android%20Gradle%20plugin
             *
             * Note that when using 'org.jetbrains.kotlin.multiplatform', we still enter this branch but it looks like `-Xjdk-release` is ignored in that case.
             * See https://youtrack.jetbrains.com/issue/KT-81606/com.android.kotlin.multiplatform.library-doesnt-error-on-Xjdk-release-usage.
             */
            freeCompilerArgs.add("-Xjdk-release=${version}")
          }
          /**
           * jvmTarget needs to be set as well, or we get an error such as
           * e: '-Xjdk-release=11' option conflicts with '-jvm-target 17'. Please remove the '-jvm-target' option
           */
          this.jvmTarget.set(version.toJvmTarget())
        }
      }
    }
  }

  override fun kotlinCompatibility(version: String) {
    val kotlinVersion = KotlinVersion.fromVersion(version.substringBeforeLast("."))
    when (kotlinProjectExtension) {
      is KotlinAndroidProjectExtension -> {
        kotlinProjectExtension.compilerOptions {
          apiVersion.set(kotlinVersion)
          languageVersion.set(kotlinVersion)
        }
      }
      is KotlinJvmProjectExtension -> {
        kotlinProjectExtension.compilerOptions {
          apiVersion.set(kotlinVersion)
          languageVersion.set(kotlinVersion)
        }
      }
      is KotlinMultiplatformExtension -> {
        val compilerOptions = compilerOptionsMethod
        if (compilerOptions != null) {
          (compilerOptions.invoke(kotlinProjectExtension) as KotlinCommonCompilerOptions).apply {
            /**
             * Kotlin 2.0+: it's important to set the version at the extension level for the shared source sets
             * like `commonMain` and `commonTest`.
             *
             * See https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-dsl-reference.html#compiler-options
             */
            apiVersion.set(kotlinVersion)
            languageVersion.set(kotlinVersion)
          }
        } else {
          /**
           * Kotlin <2.0: not sure how we do the same thing. The IDE won't be able to get the proper information in
           * common source sets, but the final binaries should still target the correct version.
           */
          kotlinProjectExtension.forEachCompilerOptions {
            apiVersion.set(kotlinVersion)
            languageVersion.set(kotlinVersion)
          }
        }
      }
    }

    /**
     * Wasm and JS require the latest kotlin stdlib
     *
     * See https://youtrack.jetbrains.com/issue/KT-66755/
     */
    val isStdlibDefaultDependencyEnabled =
      providers.gradleProperty("kotlin.stdlib.default.dependency")
        .map { it.toBooleanStrictOrNull() != false }
        .getOrElse(true)

    if (isStdlibDefaultDependencyEnabled) {
      /**
       * Downgrade the JVM stdlib version to avoid leaking incompatible metadata
       *
       * See https://github.com/Jetbrains/kotlin/blob/7fa1c5fdc7077e52d29505c6fa10a82a43665d7c/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/internal/stdlibDependencyManagement.kt#L97
       */
      if (kotlinProjectExtension is KotlinMultiplatformExtension) {
        kotlinProjectExtension.targets.configureEach { target ->
          target.compilations.withoutTests().configureEach { compilation ->
            compilation.defaultSourceSet.dependencies {
              if (target.platformType in setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm, KotlinPlatformType.common)) {
                api("org.jetbrains.kotlin:kotlin-stdlib:${version}")
              } else {
                // Non-JVM targets do not support compatibility flags and require the latest version of kotlin-stdlib
                api("org.jetbrains.kotlin:kotlin-stdlib:${kgpVersion}")
              }
            }
          }
        }
      } else {
        /**
         * Android/JVM: adding the stdlib through the compilations does not seem to work,
         * just add to the `api` configuration
         *
         * Just like in KGP, add this to all targets/compilations/sourceSets. This is especially
         * useful for tapmoc itself, which has several compilations for AGP vs KGP
         */
        kotlinProjectExtension.forAllTargets { target ->
          target.compilations.withoutTests().configureEach { compilation ->
            compilation.allKotlinSourceSets.forEach { sourceSet ->
              dependencyHandler.add(sourceSet.apiConfigurationName, "org.jetbrains.kotlin:kotlin-stdlib:${version}")
            }
          }
        }
        /**
         * We also need to fight AGP there: https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/utils/kgpUtils.kt;l=386;drc=24548ccac3813558373462f04d14e6738141624d
         *
         * This is assuming there will always be an "api" configuration...
         */
        dependencyHandler.add("api", "org.jetbrains.kotlin:kotlin-stdlib:${version}")
      }
    }
  }

  // from https://github.com/Jetbrains/kotlin/blob/4a0bfcd842ecef22ed176df3d204ec1531fa8441/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/utils/kotlinExtensionUtils.kt#L23
  private fun KotlinProjectExtension.forAllTargets(action: (target: KotlinTarget) -> Unit) {
    when (this) {
      is KotlinSingleTargetExtension<*> -> action(target)
      is KotlinMultiplatformExtension -> targets.all(action)
      else -> error("Unexpected 'kotlin' extension $this")
    }
  }

  override fun version(project: Project): String {
    return project.getKotlinPluginVersion()
  }
}

private fun <T: KotlinCompilation<*>> NamedDomainObjectContainer<T>.withoutTests(): NamedDomainObjectSet<T> {
  return matching {
    /**
     * TODO: refine this, KGP has a lot of custom logic to avoid adding to the api configuration for tests
     */
    !it.name.lowercase().contains("test")
  }
}

/**
 * See https://github.com/JetBrains/kotlin/blob/32a701d1ee0ffc79eb189b911acc5df4008e9ed4/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/utils/kotlinExtensionUtils.kt#L23
 */
private fun KotlinProjectExtension.forEachTarget(block: (KotlinTarget) -> Unit) {
  when (this) {
    is KotlinTargetsContainer -> targets.configureEach(block)
    is KotlinSingleTargetExtension<*> -> block(target)
    else -> error("Unsupported Kotlin project extension type: ${this::class.simpleName}")
  }
}

private fun Int.toJvmTarget(): JvmTarget {
  return when (this) {
    8 -> JvmTarget.JVM_1_8
    else -> JvmTarget.fromTarget(this.toString())
  }
}

internal fun KotlinProjectExtension.forEachCompilerOptions(block: KotlinCommonCompilerOptions.(platformType: KotlinPlatformType) -> Unit) {
  when (this) {
    is KotlinJvmProjectExtension -> compilerOptions.block(KotlinPlatformType.jvm)
    is KotlinAndroidProjectExtension -> compilerOptions.block(KotlinPlatformType.androidJvm)
    is KotlinMultiplatformExtension -> {
      targets.configureEach { target ->
        target.compilations.configureEach {
          it.compileTaskProvider.configure {
            it.compilerOptions.block(target.platformType)
          }
        }
      }
    }
    else -> error("Unknown kotlin extension $this")
  }
}

/**
 * calls [block] if KGP is applied
 */
internal fun Project.onKgp(block: (Kgp) -> Unit) {
  // Guard against Java-only projects
  try {
    Class.forName("org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin")
  } catch (_: ClassNotFoundException) {
    return
  }

  var hasKgp = false
  /**
   *  See https://github.com/gradle/gradle/issues/34995
   */
  plugins.withType(KotlinBasePlugin::class.java).configureEach {
    if(!hasKgp)  {
      hasKgp = true
      block(KgpImpl(this.dependencies, extensions.getByName("kotlin"), providers, getKotlinPluginVersion()))
    }
  }
}

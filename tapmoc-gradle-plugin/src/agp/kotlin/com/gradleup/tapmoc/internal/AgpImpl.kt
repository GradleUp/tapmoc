package com.gradleup.tapmoc.internal

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin

private class AgpImpl(private val project: Project): Agp {
  override fun javaCompatibility(javaVersion: JavaVersion) {
    val android = project.extensions.findByName("android") as CommonExtension<*,*,*,*,*>?
    if (android != null) {
      android.compileOptions.apply {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
      }
    } else {
      /**
       * There is androidComponents {} but not android {} => This is the `com.android.kotlin.multiplatform.library` plugin
       * do nothing here, there is no compileJava task
       */
    }
  }
}


/**
 * calls [block] if AGP is applied
 */
internal fun Project.onAgp(block: (Agp) -> Unit) {
  var hasAgp = false
  val callback = Action<AppliedPlugin> {
    if(!hasAgp)  {
      hasAgp = true
      block(AgpImpl(this@onAgp))
    }
  }
  pluginManager.withPlugin("com.android.application", callback)
  pluginManager.withPlugin("com.android.library", callback)
  pluginManager.withPlugin("com.android.test", callback)
  pluginManager.withPlugin("com.android.dynamic-feature", callback)
  pluginManager.withPlugin("com.android.kotlin.multiplatform.library", callback)
}

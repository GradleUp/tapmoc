package com.gradleup.tapmoc

import com.gradleup.tapmoc.internal.TapmocExtensionImpl
import gratatouille.GPlugin
import org.gradle.api.Project

@GPlugin(id = "com.gradleup.tapmoc")
internal fun tapmocPlugin(target: Project) {
  target.extensions.create(
    TapmocExtension::class.java,
    "tapmoc",
    TapmocExtensionImpl::class.java,
    target,
  )
}

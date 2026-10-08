package com.gradleup.tapmoc.task

import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.util.zip.ZipInputStream

@GTask
internal fun tapmocCheckClassFileVersions(
  logger: GLogger,
  severity: String,
  jarFiles: GInputFiles,
  javaVersion: Int?,
  output: GOutputFile,
) {
  if (severity == "IGNORE") {
    output.writeText("Tapmoc: check skipped as severity is IGNORE")
    return
  }
  if (javaVersion == null) {
    output.writeText("Tapmoc: skip checking class file versions as no target Java version is defined")
    return
  }
  val maxAllowedClassFileVersion = 44 + javaVersion

  jarFiles.forEach { fileWithPath ->
    ZipInputStream(fileWithPath.file.inputStream()).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory
          && entry.name.endsWith(".class")
          && !entry.name.equals("module-info.class") // See https://github.com/GradleUp/tapmoc/issues/108
          && !entry.name.startsWith("META-INF/versions")
          && !entry.name.startsWith("org/gradle/internal/impldep/META-INF/versions/") // See https://github.com/gradle/gradle/issues/24515
        ) {
          val classBytes = zis.readBytes()
          val cr = ClassReader(classBytes)
          var classFileVersion = -1.0

          cr.accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?,
              ) {
                val minor = version.shr(16)
                val major = version and 0xFFFF

                // See https://javaalmanac.io/bytecode/versions/
                classFileVersion = "$major.$minor".toDouble()
              }
            },
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
          )

          if (classFileVersion > maxAllowedClassFileVersion) {
            val foundJavaVersion = if (classFileVersion >= 49) {
              (classFileVersion - 44).toInt()
            } else {
              when (classFileVersion) {
                45.0 -> "1.0"
                45.3 -> "1.1"
                46.0 -> "1.2"
                47.0 -> "1.3"
                48.0 -> "1.4"
                else -> error("Unknown class file version: $classFileVersion")
              }
            }
            val humanReadable = "class file version $classFileVersion (Java ${foundJavaVersion})"
            val expectedHuman = "<= $maxAllowedClassFileVersion (Java $javaVersion)"
            val extra = if (fileWithPath.file.name.startsWith("gradle-api")) {
              "\nIf you are using the `java-gradle-plugin` plugin, see https://github.com/GradleUp/Tapmoc/issues/69 for more details and workarounds."
            } else {
              ""
            }
            logger.logOrFail(
              severity,
              "${fileWithPath.file.path}:${entry.name} targets $humanReadable which is newer than supported $expectedHuman.$extra",
            )
          }
        }
        entry = zis.nextEntry
      }
    }
  }

  output.writeText("Nothing to see here, this file is just a marker that the task executed successfully.")
}


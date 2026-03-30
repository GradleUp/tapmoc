#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://dl.google.com/android/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
//@file:Repository("file://~/.m2/repository")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.2.2-SNAPSHOT-b89fe292839b757bf152e8369a88991052d32d0b")

import com.gradleup.librarian.repo.updateRepo


updateRepo(
  setVersionInDocs = {
    file("README.md") {
      replacePluginVersion("com.gradleup.tapmoc")
    }
  },
)

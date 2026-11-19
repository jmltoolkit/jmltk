/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
plugins {
    id("standard-kotlin")
    application
}

dependencies {
    implementation(libs.gson)
}

application {
    mainClass = "io.github.jmltoolkit.bench.MainKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
}

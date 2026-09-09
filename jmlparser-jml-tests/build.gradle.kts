plugins {
    id("standard-kotlin")
}

description = "io.github.jmltoolkit:jmlparser-jml-tests"

dependencies {
    testImplementation(project(":jmlparser-core"))
    testImplementation(project(":jmlparser-symbol-solver-core"))
    testImplementation(project(":jmlparser-core-testing"))
    testImplementation(project(":jmlparser-core-testing-bdd"))
    testImplementation(project(":jmlparser-symbol-solver-testing"))
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.bundles.testing.runtime)
    testImplementation(kotlin("test"))
}

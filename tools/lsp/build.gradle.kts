plugins {
    id("standard-kotlin")
    kotlin("plugin.serialization") version "2.4.10"
}

version = "1.0-SNAPSHOT"

dependencies {
    api(project(":jmlparser-symbol-solver-core"))

    testImplementation(kotlin("test"))
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(kotlin("serialization"))

    implementation(project(":tools:utils"))
    implementation(project(":tools:smt"))
    implementation(project(":tools:wd"))
    implementation(project(":tools:stat"))
    implementation(project(":tools:redux"))
    implementation(project(":tools:lint"))
    implementation(project(":tools:jml2java"))

    implementation(libs.tinylog.api.kotlin)
    implementation(libs.tinylog.api)
    implementation(libs.tinylog.impl)

    implementation(libs.eclipse.lsp4j)

    implementation(libs.key.core)
    implementation(libs.key.ui)

    implementation(libs.clickt)
}

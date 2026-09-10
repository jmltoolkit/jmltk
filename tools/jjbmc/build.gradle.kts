plugins {
    id("standard-kotlin")
    application
}

dependencies {
    implementation(project(":jmlparser-symbol-solver-core"))
    implementation(libs.clickt)
    implementation(libs.snakeyaml)
}

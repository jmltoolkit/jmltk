plugins {
    id("standard-kotlin")
}

dependencies {
    api(project(":jmlparser-core"))
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.bundles.testing.runtime)
}

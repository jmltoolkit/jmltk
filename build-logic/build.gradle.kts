plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless)
    implementation(libs.dokka)
    implementation(libs.vanniktech.maven)
    implementation(libs.gradle.versions)
    implementation(libs.jacoco.cobertura)
    implementation(libs.errorprone)
}

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

    // https://github.com/Kotlin/dokka
    // Dokka is a documentation engine for Kotlin like JavaDoc for Java
    // add("implementation", libs.findLibrary("dokka-gradle").get())

    // https://detekt.dev/docs/gettingstarted/gradle/
    // A static code analyzer for Kotlin
    // add("implementation", libs.findLibrary("detekt-gradle").get())
}

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

plugins {
    kotlin("jvm")
    id("buildlogic.java-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(libs.findBundle("testing").get())
    testRuntimeOnly(libs.findBundle("testing-runtime").get())
}

kotlin {
    jvmToolchain(21)
}


configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        ktlint().setEditorConfigPath("$rootDir/.editorconfig")
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile("$rootDir/gradle/header", "(package|import|//)")
    }

    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.gradle.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
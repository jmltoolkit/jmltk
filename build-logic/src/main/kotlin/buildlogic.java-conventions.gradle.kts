@file:Suppress("UnstableApiUsage")

import net.ltgt.gradle.errorprone.errorprone
import com.diffplug.spotless.LineEnding
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier


plugins {
    `java-library`
    id("test-report-aggregation")
    id("jacoco-report-aggregation")
    id("com.diffplug.spotless")
    checkstyle
    `jvm-test-suite`
    id("com.github.ben-manes.versions")
    id("org.jetbrains.dokka")
    jacoco
    id("net.ltgt.errorprone")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "io.github.jmltoolkit"
version = providers.gradleProperty("version").getOrElse("unspecified")

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.errorprone:error_prone_core:2.50.0")
}

// Apply a specific Java toolchain to ease working on different environments.

java {
    // Auto JDK setup
    toolchain {
        libs.findVersion("jdk").ifPresent {
            languageVersion.set(JavaLanguageVersion.of(it.toString()))
        }
    }
    //withSourcesJar()
    //withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")

    // See: https://docs.oracle.com/en/java/javase/12/tools/javac.html
    options.compilerArgs.add("-Xlint:all")
    //"-Werror", // Terminates compilation when warnings occur.
    options.errorprone {
        disableWarningsInGeneratedCode = true
    }
}

tasks.withType<Javadoc> {
    val options = options as StandardJavadocDocletOptions
    options.encoding = "UTF-8"
    isFailOnError = false
    options.addBooleanOption("Xdoclint:none", true)
    options.addBooleanOption("html5", true)
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        maxHeapSize = "8g"
                        //jvmArgs("-Xmx2g")
                        //finalizedBy(tasks.jacocoTestReport)
                    }
                }
            }
        }
    }
}


configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        targetExclude("build/generated-src/**")
        toggleOffOn()
        lineEndings = LineEnding.UNIX
        palantirJavaFormat(libs.findVersion("palantirJavaFormat").get().toString())
            .formatJavadoc(false)
            .style("PALANTIR")
        importOrder("", "java", "javax", "\\#")
        licenseHeaderFile("$rootDir/gradle/header", "(package|import|//)")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

configure<CheckstyleExtension> {
    toolVersion = libs.findVersion("checkstyleVersion").get().toString()
    configFile = file("$rootDir/dev-files/JavaParser-CheckStyle.xml")
    isShowViolations = true
}

tasks.checkstyleMain {
    source("src/main/java")
    exclude("*/build/*")
}

dokka {
    dokkaSourceSets {
        configureEach {
            documentedVisibilities.set(setOf(VisibilityModifier.Protected))
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(false)
            //samples.from("samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                remoteUrl("https://github.com/jmltoolkit/jmltk/tree/main/")
                remoteLineSuffix.set("#L")
                localDirectory.set(rootDir)
            }
            perPackageOption {
                // Package options section
            }
            externalDocumentationLinks {
                externalDocumentationLinks.register("guava") {
                    url("https://javadoc.io/doc/com.google.guava/guava/latest/")
                    packageListUrl("https://javadoc.io/doc/com.google.guava/guava/33.0.0-jre/element-list")
                }
            }
        }
    }

    dokkaPublications.html {
        //moduleName.set()
        val exists = layout.projectDirectory.file("README.md").asFile
        if (exists.exists()) {
            includes.from("README.md")
        }
    }
}

// To generate documentation in HTML
val dokkaHtmlJar = tasks.register<Jar>("dokkaHtmlJar") {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}


jacoco {
    toolVersion = "0.8.15"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

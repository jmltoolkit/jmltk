@file:Suppress("UnstableApiUsage")

import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier


plugins {
    `java-library`
    id("test-report-aggregation")
    id("com.diffplug.spotless")
    checkstyle
    id("com.github.ben-manes.versions")
    id("org.jetbrains.dokka")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "io.github.jmltoolkit"
version = project.properties["version"] ?: "unspecified"

repositories {
    mavenCentral()
}

dependencies {
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
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        maxHeapSize = "8g"
                        //jvmArgs("-Xmx2g")
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
        removeUnusedImports()
        trimTrailingWhitespace()
        palantirJavaFormat(libs.findVersion("palantirJavaFormat").get().toString()).formatJavadoc(false)
            .style("PALANTIR")
        licenseHeaderFile("$rootDir/gradle/header", "(package|import|//)")
    }
}

configure<CheckstyleExtension> {
    toolVersion = libs.findVersion("checkstyleVersion").get().toString()
    configFile = file("$rootDir/dev-files/JavaParser-CheckStyle.xml")
    isShowViolations = true
}

tasks.checkstyleMain {
    source("src/main/java")
    exclude("**/build/generated-src/**")
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
val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

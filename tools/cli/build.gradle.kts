plugins {
    id("standard-kotlin")
    application
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.shadow)
}

application {
    mainClass = "io.github.jmltoolkit.cli.MainKt"
    applicationName = "jmltk"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

graalvmNative {
    toolchainDetection.set(false)
    
    binaries {
        named("main") {
            imageName.set("jmltk")
            mainClass = application.mainClass
            buildArgs.addAll(
                "--enable-native-access=ALL-UNNAMED",
                "-O3",
                "--no-fallback"
            )
            quickBuild = true
        }

        named("test") {
            // options to configure the test binary
            quickBuild = true
            debug = true
        }

    }
}

distributions {
    main {
        contents {
            from("$rootDir/README.md") {
                into(".")
            }
            from("$rootDir/LICENSE") {
                into(".")
            }
        }
    }
}

dependencies {
    implementation(libs.clickt)
    implementation(project(":jmlparser-core"))
    implementation(project(":tools:wd"))
    implementation(project(":tools:xpath"))
    implementation(project(":tools:prettyprinting"))
    implementation(project(":tools:lint"))
    implementation(project(":tools:stat"))
    implementation(project(":tools:jml2java"))
    implementation(project(":tools:jmlstub"))
}

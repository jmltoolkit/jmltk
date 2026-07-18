plugins {
    id("standard-kotlin")
    application
}

application {
    mainClass = "io.github.jmltoolkit.cli.MainKt"
    applicationName = "jmltk"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
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

/*
tasks.named("startScripts") {
    doLast {
        def unixScript = file("$outputDir/$applicationName")
        unixScript.text = unixScript.text.replace(
            'DEFAULT_JVM_OPTS=',
            'DEFAULT_JVM_OPTS=\'--enable-native-access=ALL-UNNAMED\' '
        )
    }
 */

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

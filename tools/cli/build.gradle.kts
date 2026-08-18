plugins {
    id("standard-kotlin")
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass = "io.github.jmltoolkit.cli.MainKt"
    applicationName = "jmltk"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "jmltk"
    defaultJvmOpts = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.register<CreateStartScripts>("startLspScripts") {
    description = "Create the jmltk-lsp start script"
    applicationName = "jmltk-lsp"
    mainClass = "io.github.jmltoolkit.lsp.Main"
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    defaultJvmOpts = listOf("--enable-native-access=ALL-UNNAMED")
    outputDir = layout.buildDirectory.file("install/jmltk/bin").get().asFile
}

//tasks.named("installDist") {
//    dependsOn(tasks.named("startLspScripts"))
//}

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

    implementation(project(":tools:lsp"))
}

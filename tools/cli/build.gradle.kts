import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

fun createLauncher(name: String, mainClassName: String) =
    tasks.register<CreateStartScripts>("${name}StartScripts") {
        description = "Create a start script for $name"
        classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
        defaultJvmOpts = listOf("--enable-native-access=ALL-UNNAMED")
        applicationName = name
        mainClass.set(mainClassName)
        classpath = files(tasks.named("jar"), configurations.runtimeClasspath)
        outputDir = layout.buildDirectory.dir("tmp/scripts").get().asFile
    }

val lspStart = createLauncher("jmltk-lsp", "io.github.jmltoolkit.lsp.Main")

distributions {
    main {
        contents {
            from("$rootDir/README.md") {
                into(".")
            }
            from("$rootDir/LICENSE") {
                into(".")
            }
            from(lspStart) {
                into("bin")
                filePermissions {
                    unix("rwxr-xr-x")
                }
            }

            from("distribution") {
                into(".")
                expand(
                    "name" to rootProject.name,
                    "version" to rootProject.version,
                    "groupId" to rootProject.group,
                    "artifactId" to "jmlparser-core",
                    "date" to LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )
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

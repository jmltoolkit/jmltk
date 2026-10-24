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
    shortenWindowsClasspathLine()
}

/** Replace Gradle's explicit CLASSPATH jar enumeration in the Windows start
 *  script with the Java classpath wildcard form. jmltk bundles 100+ jars, and
 *  the generated `set CLASSPATH=...` line exceeds cmd.exe's 8191-character line
 *  limit on Windows, making the launcher unusable (`The input line is too long`).
 */
fun CreateStartScripts.shortenWindowsClasspathLine() {
    doLast {
        val file = windowsScriptFile.get().asFile
        val script = file.readText()
        val fixed = script.replace(
            Regex("(?m)^set CLASSPATH=.*\r?\n"),
            "set CLASSPATH=%APP_HOME%\\\\lib\\\\*\r\n",
        )
        if (fixed != script) file.writeText(fixed)
    }
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
        shortenWindowsClasspathLine()
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

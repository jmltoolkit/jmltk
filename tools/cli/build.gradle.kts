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

/** Keep the Windows start script below cmd.exe's 8191-character line limit.
 *
 *  jmltk bundles 100+ jars, and Gradle's `set CLASSPATH=...` line exceeds that
 *  limit on Windows, making the launcher unusable (`The input line is too long`).
 *  There is no way to emit an explicit jar list here: cmd.exe expands
 *  `%APP_HOME%` when the `set` executes, so the accumulated absolute-path
 *  classpath (>8k chars for 115 jars plus each platform's workspace prefix)
 *  blows the limit again on every continuation line, no matter how it is
 *  chunked. The `%APP_HOME%\lib\*` wildcard keeps the script short and defers
 *  expansion to the JVM.
 *
 *  A wildcard is only safe if every class exists exactly once: Java expands it
 *  in directory-enumeration order, and the first jar to provide a class wins.
 *  The CLI therefore excludes KeY's proofjava fork core
 *  (`org.key-project.proofjava:javaparser-core`, pulled in transitively by the
 *  LSP's key.core dependency) — see the `:tools:lsp` dependency below — so the
 *  distribution carries exactly one `com.github.javaparser` implementation, the
 *  JMLtk fork core, and directory order cannot select the wrong
 *  `ParserConfiguration`.
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
    implementation(project(":tools:vcg"))

    // The LSP bundles KeY (key.core/key.ui) for its KeY integration actions.
    // KeY's own JavaParser fork (org.key-project.proofjava:javaparser-core)
    // duplicates the com.github.javaparser classes the rest of the CLI uses
    // from the JMLtk fork core. The Windows start script resolves jars via the
    // lib\* wildcard in directory order, so a duplicate ParserConfiguration is
    // a coin toss — it crashed 'jmltk vcg' with NoSuchMethodError on
    // setProcessJml. Ship exactly one com.github.javaparser implementation:
    // the JMLtk fork core.
    implementation(project(":tools:lsp")) {
        exclude(group = "org.key-project.proofjava")
    }
}

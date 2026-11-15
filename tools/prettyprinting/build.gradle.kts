plugins {
    id("standard-kotlin")
    application
}

val googleJavaFormatExports =
    listOf(
        "--add-exports",
        "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports",
        "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports",
        "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports",
        "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports",
        "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    )

application {
    mainClass = "io.github.jmltoolkit.prettyprinting.MainKt"
    applicationName = "jmltk-pp"
    applicationDefaultJvmArgs = googleJavaFormatExports
}

dependencies {
    implementation(project(":tools:utils"))
    implementation(libs.clickt)
    implementation(libs.google.java.format)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(googleJavaFormatExports)
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test {
    jvmArgs(googleJavaFormatExports)
}

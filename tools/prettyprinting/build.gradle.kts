plugins {
    id("standard-kotlin")
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

dependencies {
    implementation(project(":tools:utils"))
    implementation(libs.google.java.format)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(googleJavaFormatExports)
}

tasks.test {
    jvmArgs(googleJavaFormatExports)
}

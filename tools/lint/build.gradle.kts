plugins {
    id("standard-kotlin")
    alias(libs.plugins.ksp)
}

dependencies {
    api(project(":jmlparser-symbol-solver-core"))
    api(libs.gson)
    implementation(libs.logback)
    implementation(libs.violations)
    testImplementation(project(":tools:utils"))

    ksp(project(":tools:build-helpers"))
}

ksp {
    arg("specialInterface", "io.github.jmltoolkit.lint.LintRule")
}

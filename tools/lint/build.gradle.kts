plugins {
    id("standard-kotlin")
    alias(libs.plugins.ksp)
}


dependencies {
    api(project(":jmlparser-symbol-solver-core"))
    api(libs.gson)
    implementation(libs.logback)
    implementation("se.bjurr.violations:violations-lib:2.2.0")
    testImplementation(project(":tools:utils"))

    ksp(project(":tools:build-helpers"))

}

ksp {
    arg("specialInterface", "io.github.jmltoolkit.lint.LintRule")
}
plugins {
    id("standard-kotlin")
}

dependencies {
    api(project(":jmlparser-symbol-solver-core"))
    api(libs.gson)
    implementation(libs.logback)
    implementation("se.bjurr.violations:violations-lib:3.0.0")
    testImplementation(project(":tools:utils"))
}

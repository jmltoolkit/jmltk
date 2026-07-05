plugins {
    id("standard-kotlin")
}

dependencies {
    api(project(":jmlparser-symbol-solver-core"))
    api(libs.gson)
    implementation(libs.logback)
    implementation("se.bjurr.violations:violations-lib:1.160.5")
    testImplementation(project(":tools:utils"))
}

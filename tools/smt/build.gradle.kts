plugins {
    id("standard-kotlin")
}

dependencies {
    // implementation(libs)
    implementation(project(":tools:utils"))
    implementation("org.sosy-lab:java-smt:6.0.0")

    // Z3 JNI wrapper and native libraries (linux x64/arm64), published as
    // classifier artifacts, see https://github.com/sosy-lab/java-smt
    implementation("org.sosy-lab:javasmt-solver-z3:4.16.0")
    implementation("org.sosy-lab:javasmt-solver-z3:4.16.0:libz3-x64@so")
    implementation("org.sosy-lab:javasmt-solver-z3:4.16.0:libz3java-x64@so")
    implementation("org.sosy-lab:javasmt-solver-z3:4.16.0:libz3-arm64@so")
    implementation("org.sosy-lab:javasmt-solver-z3:4.16.0:libz3java-arm64@so")

    testImplementation(libs.snakeyaml)

}

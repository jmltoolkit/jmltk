plugins {
    id("standard-kotlin")
}

dependencies {
    api(project(":jmlparser-symbol-solver-core"))
    api(project(":tools:smt"))

    implementation("de.uni-freiburg.informatik.ultimate:smtinterpol:2.5-1388-ga5a4ab0c")
}

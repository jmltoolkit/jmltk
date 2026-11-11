plugins {
    id("standard-kotlin")
}

dependencies {
    api(project(":tools:smt"))
    implementation(project(":tools:utils"))
}

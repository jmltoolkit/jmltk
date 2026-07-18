plugins {
    id("standard-kotlin")
}

dependencies {
    api(project(":tools:utils"))
    api(libs.org.javassist.javassist)
    api(project(":jmlparser-core"))
}

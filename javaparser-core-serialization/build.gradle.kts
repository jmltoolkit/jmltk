plugins {
    id("buildlogic.java-conventions")
    id("publish-convention")
}

description = "Serialization support for the JMLtk JavaParser AST."

dependencies {
    api(project(":jmlparser-core"))
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.bundles.testing)

    api(libs.jakarta.json.jakarta.json.api)
    testImplementation(libs.org.eclipse.parsson.parsson)
}



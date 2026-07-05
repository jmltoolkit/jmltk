plugins {
    id("com.diffplug.spotless") version "8.8.0" apply false
    id("standard-kotlin") apply false
    alias(libs.plugins.dokka)
}

repositories { mavenCentral() }

dependencies {
    /*subprojects.forEach {
        dokka(it)
    }*/
    dokka(project(":jmlparser-jml-tests"))
    dokka(project(":javaparser-key-testing"))
    dokka(project(":jmlparser-core-serialization"))
    dokka(project(":jmlparser-core-testing"))
    dokka(project(":jmlparser-core-testing-bdd"))
    dokka(project(":jmlparser-core-metamodel-generator"))
    dokka(project(":jmlparser-core-generators"))
    dokka(project(":jmlparser-core"))
    dokka(project(":jmlparser-symbol-solver-core"))
    dokka(project(":jmlparser-symbol-solver-testing"))
    dokka(project(":tools:stat"))
    dokka(project(":tools:lint"))
    dokka(project(":tools:smt"))
    dokka(project(":tools:prettyprinting"))
    dokka(project(":tools:utils"))
    dokka(project(":tools:wd"))
    dokka(project(":tools:redux"))
    dokka(project(":tools:xpath"))
    dokka(project(":tools:cli"))
    dokka(project(":tools:lsp"))
    dokka(project(":tools:web"))
    dokka(project(":tools:jml2java"))
}


dokka {
    dokkaPublications.html {
        includes.from(file("README.md"))
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)
        offlineMode.set(false)
    }
}
plugins {
    id("buildlogic.java-conventions")
    id("publish-convention")
}

description = "Symbol solving for the JMLtk JavaParser AST."

dependencies {
    api(project(":jmlparser-core"))
    api(libs.org.javassist.javassist)
    api(libs.com.google.guava.guava)
    api(libs.org.checkerframework.checker.qual)
}



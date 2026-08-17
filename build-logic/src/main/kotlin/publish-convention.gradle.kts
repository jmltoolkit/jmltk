import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.vanniktech.maven.publish")
    signing
}


publishing {
    repositories {
        maven {
            name = "LOCAL"
            url = uri("$rootDir/local")
        }
        maven {
            // deployment to git.key-project.org/key-public/key
            name = "KEYLAB"
            url = uri("https://git.key-project.org/api/v4/projects/35/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                val userToken = envOrPropertyValue("GITLAB_USER_TOKEN")
                val deployToken = envOrPropertyValue("GITLAB_DEPLOY_TOKEN")
                val ciToken = envOrPropertyValue("GITLAB_CIJOB_TOKEN")

                if (userToken != "") {
                    name = "Private-Token"
                    value = userToken
                } else if (deployToken != "") {
                    name = "Deploy-Token"
                    value = deployToken
                } else {
                    name = "Job-Token"
                    value = ciToken
                }
            }
            authentication {
                create("basic", HttpHeaderAuthentication::class)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    //signAllPublications()

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = SourcesJar.Sources(),
        ),
    )

    pom {
        licenses {
            license {
                name = "The GNU General Public License 3"
                url = "http://www.gnu.org/licenses/gpl-3.0.html"
            }
        }
        developers {
            developer {
                id = "weigl"
                name = "Alexander Weigl"
                email = "weigl@kit.edu"
                url = "https://formal.iti.kit.edu/~weigl"
            }

        }
        scm {
            connection = "scm:git:https://github.com/jmltoolkit/jmltk.git"
            developerConnection = "scm:git:ssh://github.com/jmltoolkit/jmltk.git"
            url = "https://github.com/jmltoolkit/jmltk"
        }
    }
}

fun envOrPropertyValue(key: String): String =
    if (key in System.getenv()) {
        System.getenv(key)
    } else {
        project.findProperty(key)?.toString() ?: ""
    }

val emptyJavadocJar = tasks.findByName("plainJavadocJar")!!
val meta = tasks.findByName("generateMetadataFileForMavenPublication")!!
meta.dependsOn(emptyJavadocJar)

signing { useGpgCmd() }
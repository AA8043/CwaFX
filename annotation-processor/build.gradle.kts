plugins {
    `maven-publish`
    signing
}

publishing {
    repositories {
        mavenLocal()
    }

    publications {
        create<MavenPublication>("maven") {
            artifactId = "cwafx-annotation-processor"
            from(components["java"])

            pom {
                name.set("cwafx-annotation-processor")
                description.set("CwaFX Annotation Processor")
                url.set("https://github.com/AA8043/CwaFX")

                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                }

                developers {
                    developer {
                        id.set(project.findProperty("developerId") as String)
                        name.set(project.findProperty("developerName") as String)
                        email.set(project.findProperty("developerEmail") as String)
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/AA8043/CwaFX.git")
                    developerConnection.set("scm:git:ssh://git@github.com/AA8043/CwaFX.git")
                    url.set("https://github.com/AA8043/CwaFX")
                }
            }
        }
    }
}

signing {
    val signingKey = project.findProperty("signingKey") as String?
    val signingPassword = project.findProperty("signingPassword") as String?
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    } else {
        useGpgCmd()
    }
    sign(publishing.publications["maven"])
}

dependencies {
    implementation(project(":core"))

    implementation("com.google.auto.service:auto-service:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

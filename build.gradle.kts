plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("io.freefair.lombok") version "8.6"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withSourcesJar()
    withJavadocJar()
}

javafx {
    version = "21.0.4"
    modules = listOf("javafx.controls", "javafx.fxml")
}

publishing {
    repositories {
        mavenLocal()
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("cwafx")
                description.set("A JavaFX framework")
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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(project.findProperty("centralUsername") as String)
            password.set(project.findProperty("centralPassword") as String)
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

group = "io.github.aa8043"
version = "1.5.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    api("io.github.typhon0:AnimateFX:1.3.0")

    api("org.slf4j:slf4j-api:2.0.18")
    api("org.apache.logging.log4j:log4j-core:2.26.0")
    api("org.apache.logging.log4j:log4j-slf4j2-impl:2.26.0")

    val hutoolVersion = "5.8.40"
    api("cn.hutool:hutool-core:$hutoolVersion")
    api("cn.hutool:hutool-json:$hutoolVersion")
    api("cn.hutool:hutool-extra:$hutoolVersion")

    implementation("com.google.auto.service:auto-service:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "io.github.aa8043"
version = "1.8.0"

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

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }

        withSourcesJar()
        withJavadocJar()
    }

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

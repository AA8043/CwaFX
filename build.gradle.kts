plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "8.6"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21.0.4"
    modules = listOf("javafx.controls", "javafx.fxml")
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AA8043/CwaFX")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

group = "org.a8043.cwaFX"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
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

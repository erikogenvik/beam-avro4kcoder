plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    `java-library`
    id("maven-publish")
}

group = "org.ogenvik.beam-avro4kcoder"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(kotlin("stdlib"))
    api("org.ogenvik.avro4k4beam:avro4k4beam-core:1.0.0-LOCAL")
    api("org.reflections:reflections:${Versions.reflections}")
    api("org.apache.beam:beam-sdks-java-core:${Versions.beam}")

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.spek}")
    testImplementation("org.amshove.kluent:kluent:${Versions.kluent}")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:${Versions.spek}")
}


tasks {
    test {
        // Use junit platform for unit tests.
        useJUnitPlatform {}
    }
}


publishing {
    repositories {
        maven {
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            name = "deploy"
            url = if (Ci.isRelease) releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: ""
                password = System.getenv("OSSRH_PASSWORD") ?: ""
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            pom {
                name.set("beam-avro4kcoder")
                description.set("Apache Beam coder that uses Avro together with kotlinx.serialization")
                url.set("https://www.github.com/erikogenvik/beam-avro4kcoder")

                scm {
                    connection.set("scm:git:https://www.github.com/erikogenvik/beam-avro4kcoder")
                    developerConnection.set("scm:git:https://github.com/erikogenvik/beam-avro4kcoder")
                    url.set("https://www.github.com/erikogenvik/beam-avro4kcoder")
                }

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("erikogenvik")
                        name.set("Erik Ogenvik")
                        email.set("erik@ogenvik.org")
                    }
                }
            }
        }
    }
}
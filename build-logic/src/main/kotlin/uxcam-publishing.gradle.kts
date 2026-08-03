import com.uxcam.buildlogic.UXCamPomExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugins.signing.Sign

// Shared Maven Central publishing convention for every published module. Consumers set the
// per-module POM name/description through the `uxcamPom` extension:
//
//     uxcamPom {
//         name.set("UXCam KMP")
//         description.set("...")
//     }
//
// Apply AFTER `java-gradle-plugin` where both are used — see the javadoc jar note below.

plugins {
    `maven-publish`
    signing
}

val uxcamPom = extensions.create<UXCamPomExtension>("uxcamPom")

// Maven Central requires a javadoc jar. `java-gradle-plugin` modules produce a real one via
// `java { withJavadocJar() }`; KMP modules have no javadoc, so ship an empty jar. The check
// relies on `java-gradle-plugin` being applied before this plugin.
if (!pluginManager.hasPlugin("java-gradle-plugin")) {
    val javadocJar = tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
    }
    publishing.publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar)
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(uxcamPom.name)
            description.set(uxcamPom.description)
            url.set("https://github.com/uxcam/uxcam-kmp")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/uxcam/uxcam-kmp/blob/main/LICENSE")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("uxcam")
                    name.set("UXCam")
                    email.set("hello@uxcam.com")
                    organization.set("UXCam")
                    organizationUrl.set("https://uxcam.com")
                }
            }
            scm {
                url.set("https://github.com/uxcam/uxcam-kmp")
                connection.set("scm:git:https://github.com/uxcam/uxcam-kmp.git")
                developerConnection.set("scm:git:ssh://git@github.com/uxcam/uxcam-kmp.git")
            }
        }
    }
    repositories {
        maven {
            name = "central"
            // OSSRH (s01.oss.sonatype.org) was decommissioned on 2025-06-30. Releases stage
            // through Sonatype's OSSRH-compatible Central Portal endpoint — after upload, finish
            // the release in the Portal UI (central.sonatype.com). Snapshots go straight to the
            // Portal snapshot repository. Credentials are Central Portal user tokens.
            setUrl(
                provider {
                    if (version.toString().endsWith("SNAPSHOT")) {
                        "https://central.sonatype.com/repository/maven-snapshots/"
                    } else {
                        "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                    }
                },
            )
            credentials {
                username = providers.gradleProperty("ossrhUsername")
                    .orElse(providers.environmentVariable("OSSRH_USERNAME")).orNull
                password = providers.gradleProperty("ossrhPassword")
                    .orElse(providers.environmentVariable("OSSRH_PASSWORD")).orNull
            }
        }
    }
}

// The signing key is optional so local builds work without it; CI provides it as a secret.
signing {
    val signingKey = providers.environmentVariable("SIGNING_KEY")
        .orElse(providers.gradleProperty("signingKey")).orNull
    val signingPassword = providers.environmentVariable("SIGNING_PASSWORD")
        .orElse(providers.gradleProperty("signingPassword")).orNull
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

// KMP + signing: publish tasks consume signature outputs, so make the dependency explicit.
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

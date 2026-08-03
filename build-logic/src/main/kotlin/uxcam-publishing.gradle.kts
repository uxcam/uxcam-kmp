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
        // Releases are staged into a local directory, zipped into a single deployment bundle,
        // and POSTed to the Central Portal Publisher API by scripts/publish-central.sh — the
        // same mechanism the native Android SDK uses.
        //
        // The alternative (PUT-ing straight at ossrh-staging-api.central.sonatype.com) works but
        // has two drawbacks we hit for real on 0.0.1: the resulting deployment is named
        // "com.uxcam (via OSSRH Staging API)" with no way to identify the artifact, and the
        // upload only becomes a deployment after a separate promotion call that the shim keys
        // by the CALLER'S IP — so it must happen on the uploading machine or the artifacts are
        // stranded. Bundle upload has neither problem and names the deployment after the zip.
        //
        // `uxcamStagingDir` lets the `gradle-plugin` included build (whose rootProject differs)
        // stage into the SAME directory, so one bundle covers the whole release.
        maven {
            name = "centralStaging"
            url = uri(
                providers.gradleProperty("uxcamStagingDir").orNull?.let { File(it) }
                    ?: rootProject.layout.buildDirectory.dir("maven-central-staging").get().asFile
            )
        }
        // Snapshots bypass staging and go straight to the Portal snapshot repository.
        maven {
            name = "centralSnapshots"
            setUrl("https://central.sonatype.com/repository/maven-snapshots/")
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

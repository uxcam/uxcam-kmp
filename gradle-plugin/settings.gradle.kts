pluginManagement {
    includeBuild("../build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
    // Reuse the root version catalog — single source of truth for the Kotlin, uxcam-kmp, and
    // native SDK versions (this is an included build, so the catalog isn't inherited).
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
rootProject.name = "uxcam-kmp-gradle-plugin"

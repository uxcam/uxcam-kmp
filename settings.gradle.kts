pluginManagement {
    includeBuild("build-logic")
    includeBuild("gradle-plugin")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // Serves the UXCam iOS XCFramework zip as a resolvable artifact. The pattern maps
        // `com.uxcam.ios:UXCam:<version>@zip` to the GitHub release zip, e.g.
        // .../uxcam-ios/releases/download/3.10.0/UXCam.xcframework.zip. Scoped to the
        // com.uxcam.ios group only. The SDK moved from the now-archived `uxcam-ios-sdk` repo
        // (zip committed in-tree) to `uxcam-ios`, which publishes it as a release asset —
        // hence the different host and path.
        ivy {
            name = "uxcamIosSdk"
            setUrl("https://github.com/uxcam/uxcam-ios/releases/download/")
            patternLayout { artifact("[revision]/[artifact].xcframework.[ext]") }
            metadataSources { artifact() }
            content { includeGroup("com.uxcam.ios") }
        }
    }
}

rootProject.name = "uxcam-kmp"

include(":uxcam-kmp")
include(":uxcam-compose")
include(":androidApp")
include(":examples:shared")

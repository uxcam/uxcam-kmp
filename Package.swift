// swift-tools-version:5.9
import PackageDescription

// `version` and `checksum` are stamped by .github/workflows/release.yml: it builds
// UXCamKMP.xcframework.zip, uploads it as the release asset for `version`, and rewrites
// both values so the tagged manifest matches the exact asset. Do not edit them by hand.
// On main the version stays at this unreleased placeholder. The release workflow stamps the
// real version and checksum into the immutable release-tag commit.
let version = "0.0.0-unreleased"
let checksum = "ace77ffccdfd5bd7a34d729b835871c5e101e2450d87ff350e9ca23eeca5b3c9"

let package = Package(
    name: "UXCamKMP",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "UXCamKMP",
            targets: ["UXCamKMPBinary", "UXCamKMPKit"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/uxcam/uxcam-ios", exact: "3.10.0")
    ],
    targets: [
        .binaryTarget(
            name: "UXCamKMPBinary",
            url: "https://github.com/uxcam/uxcam-kmp/releases/download/v\(version)/UXCamKMP.xcframework.zip",
            checksum: checksum
        ),
        .target(
            name: "UXCamKMPKit",
            dependencies: [
                "UXCamKMPBinary",
                .product(name: "UXCam", package: "uxcam-ios")
            ],
            path: "spm/UXCamKMPKit"
        )
    ]
)

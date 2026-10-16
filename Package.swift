// swift-tools-version:5.9
import PackageDescription

// `version` and `checksum` are stamped by .github/workflows/release.yml: it builds
// UXCamKMP.xcframework.zip, uploads it as the release asset for `version`, and rewrites
// both values so the tagged manifest matches the exact asset. Do not edit them by hand.
// On main the version stays at this unreleased placeholder. The release workflow stamps the
// real version and checksum into the immutable release-tag commit.
let version = "1.0.1"
let checksum = "0354675e9828c84717597caa7598f7bc3e02d13786a9ee94fe399a27a2fc23db"

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
        .package(url: "https://github.com/uxcam/uxcam-ios", exact: "3.11.1")
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

// swift-tools-version:5.9
import PackageDescription

// `version` and `checksum` are stamped by .github/workflows/release.yml: it builds
// UXCamKMP.xcframework.zip, uploads it as the release asset for `version`, and rewrites
// both values so the tagged manifest matches the exact asset. Do not edit them by hand.
// Until the first release is published this URL does not resolve — build from source
// instead (scripts/build-ios.sh) or use scripts/package-xcframework.sh for a local zip.
let version = "0.0.1"
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
            url: "https://github.com/uxcam/uxcam-kmp/releases/download/\(version)/UXCamKMP.xcframework.zip",
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

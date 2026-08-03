// swift-tools-version:5.9
import PackageDescription

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
            path: "Artifacts/UXCamKMP.xcframework"
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

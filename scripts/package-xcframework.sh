#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

OUT_DIR="uxcam-kmp/build/XCFrameworks/release"
ZIP="$OUT_DIR/UXCamKMP.xcframework.zip"
VENDORED_DIR="Artifacts/UXCamKMP.xcframework"

echo "==> Building UXCamKMP.xcframework"
./gradlew :uxcam-kmp:assembleUXCamKMPXCFramework

echo "==> Refreshing the local SwiftPM binary artifact"
mkdir -p "$VENDORED_DIR"
rsync -a --delete "$OUT_DIR/UXCamKMP.xcframework/" "$VENDORED_DIR/"

echo "==> Zipping XCFramework"
rm -f "$ZIP"
( cd "$OUT_DIR" && zip -qry "UXCamKMP.xcframework.zip" "UXCamKMP.xcframework" )

echo "==> Computing SPM checksum"
CHECKSUM="$(swift package compute-checksum "$ZIP")"

cat <<EOF

==================================================================
  Artifact : $ROOT/$ZIP
  Checksum : $CHECKSUM

  Upload the zip to a GitHub release, then in Package.swift replace
  the local binary target with:

    .binaryTarget(
        name: "UXCamKMPBinary",
        url: "https://github.com/uxcam/uxcam-kmp/releases/download/<version>/UXCamKMP.xcframework.zip",
        checksum: "$CHECKSUM"
    )
==================================================================
EOF

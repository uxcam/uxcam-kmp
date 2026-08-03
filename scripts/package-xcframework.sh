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

  Releases are normally cut by .github/workflows/release.yml, which
  rebuilds this zip, uploads it as the GitHub release asset, and
  stamps Package.swift's \`version\` + \`checksum\` to match.

  For a manual/emergency release, upload the zip to the GitHub
  release for <version> and set in Package.swift:

    let version = "<version>"
    let checksum = "$CHECKSUM"
==================================================================
EOF

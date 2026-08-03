#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

./gradlew :uxcam-kmp:assembleUXCamKMPXCFramework

echo "==> UXCamKMP.xcframework at uxcam-kmp/build/XCFrameworks/release/UXCamKMP.xcframework"

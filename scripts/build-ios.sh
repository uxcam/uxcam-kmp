#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> [1/3] Assembling Shared.xcframework (:examples:shared, via the plugin)"
./gradlew :examples:shared:assembleSharedXCFramework

echo "==> [2/3] Generating iosApp.xcodeproj"
( cd iosApp && xcodegen generate )

echo "==> [3/3] Building iosApp for the iOS Simulator"
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug \
  -derivedDataPath iosApp/build \
  build

echo "==> Done. App built at iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app"

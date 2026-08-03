## Checklist

- [ ] `./gradlew jvmTest` and `./gradlew -p gradle-plugin test` pass locally
- [ ] Native SDK versions changed only via `gradle/libs.versions.toml` (with `uxcamIosChecksum` updated to match)
- [ ] `Package.swift` was not edited by hand — `version`/`checksum` are stamped by the release workflow
- [ ] No binary artifacts are tracked (XCFrameworks ship as release assets)
- [ ] Sample app keys remain the `YOUR_UXCAM_APP_KEY` placeholder

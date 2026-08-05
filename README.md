# uxcam-kmp

A Kotlin Multiplatform library for [UXCam](https://uxcam.com) — session recording and product
analytics through a single shared Kotlin API that delegates to the native UXCam SDK on each platform.

| Platform | Native SDK | Delivered via |
|----------|------------|---------------|
| Android  | `com.uxcam:uxcam` | Maven Central |
| iOS      | `uxcam-ios`   | Swift Package Manager or CocoaPods |

## Installation

### Kotlin Multiplatform

Apply the Gradle plugin to your shared module:

```kotlin
plugins {
    id("com.uxcam.kmp") version "<version>"
}
```

The plugin adds the `uxcam-kmp` dependency to `commonMain` (and `uxcam-compose` when a Compose
plugin is detected). As with any multi-module KMP + Android project, declare the Kotlin and
Android plugins once in the root build script with `apply false` — without that, sibling modules
load the Kotlin plugin in separate classloaders and Kotlin/Native task creation fails with a
"shared build service" error. Android resolves its native SDK transitively. On iOS, link the native UXCam
SDK once at the app boundary through SwiftPM or CocoaPods:

| Your iOS setup | What the plugin does | Extra steps |
|---|---|---|
| Default template (embed-and-sign, static framework) | Leaves native UXCam symbols for the final Xcode app link | Add `https://github.com/uxcam/uxcam-ios` to the iOS app |
| Dynamic Kotlin framework | Not recommended with a separately linked static native SDK because it can duplicate UXCam classes | Use a static Kotlin framework |
| CocoaPods (`kotlin("native.cocoapods")`) | Adds `pod("UXCam")` (link-only), floors the deployment target, adds Swift-compat search paths, and warns if a dynamic framework would duplicate the SDK | **None** — just run `pod install` as usual |
| SwiftPM (`uxcam-ios` package added in Xcode) | Xcode supplies the native SDK, linker settings, and privacy manifest at the final app link | **None** beyond having added the package |

On Android the native `com.uxcam:uxcam` SDK arrives transitively through Gradle — nothing to do.
The plugin also fails fast with an actionable message when the consumer's Kotlin version is too
old for the published klib, warns when the Kotlin plugin is loaded by multiple classloaders (the
missing root `apply false` mistake), reminds you about the Xcode-side SwiftPM link, and skips iOS
work on non-Mac hosts. It does not download, merge, or rewrite Apple frameworks.

Its knobs (all optional, set via `uxcamKmp { ... }`):

| Knob | Default | Purpose |
|---|---|---|
| `verifyKotlinVersion` | `true` | Fail fast on a too-old consumer Kotlin |
| `installComposeHelpers` | `true` | Auto-add `uxcam-compose` when a Compose plugin is detected |
| `exportToIosFrameworks` | `false` | Export `UXCamKMP` into your framework so Swift can call it directly (instead of through your own shared facade) |
| `iosLinkReminder` | `true` | Log the SwiftPM app-link reminder for iOS builds without CocoaPods |
| `libraryVersion` | plugin's own | Override the installed library version (hotfixes) |

Prefer manual control? Depend on the library directly (you then own the iOS linking):

```kotlin
dependencies {
    implementation("com.uxcam:uxcam-kmp:<version>")
}
```

Both supported iOS paths let Xcode process UXCam's `PrivacyInfo.xcprivacy`.

### iOS (native app)

Add the Swift package, then `import UXCamKMP`:

```
https://github.com/uxcam/uxcam-kmp
```

The package contains the prebuilt KMP XCFramework and pins the native `uxcam-ios` version used
to generate its Objective-C bindings.

## Usage

```kotlin
import com.uxcam.kmp.KMPUXCamBlur
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.uxcamConfiguration

UXCamKMP.startWithConfiguration(
    uxcamConfiguration("YOUR_UXCAM_APP_KEY") {
        enableCrashHandling = true
        occlusions = listOf(KMPUXCamBlur(screens = listOf("PaymentScreen")))
    }
)

UXCamKMP.tagScreenName("Home")
UXCamKMP.logEvent("checkout_started", mapOf("cart_size" to 3))
UXCamKMP.setUserIdentity("user-42")
UXCamKMP.setSessionProperty("ab_bucket", "checkout_v2")
UXCamKMP.reportBugEvent("payment_declined", mapOf("code" to 402))
```

The common API covers the native surface shared by Android and iOS: session lifecycle, screen
tagging + ignore lists,
event logging (map/JSON), bug & exception reporting, user and session properties, structured
occlusion (overlay/blur, per-screen rules), recording control incl. short breaks, opt-in/out
(overall + video), verification listeners, pending-upload management, and dashboard URLs.
Android-only APIs are Android source-set extensions, so unsupported calls cannot silently compile
and do nothing on iOS.

Runtime occlusion is additive on both platforms: `applyBlurOcclusion`/`applyOverlayOcclusion`
stack on top of rules already active, and `removeOcclusion()` clears them all. One iOS caveat:
rules applied *before* `startWithConfiguration` are folded into the startup configuration (the
only point where the native iOS SDK honours `excludeMentionedScreens`), and the native SDK
cannot remove configuration-based occlusion at runtime — pass short-lived rules after start
instead.

Besides Android and iOS, the library ships no-op binaries for `jvm`, `js`, `wasmJs`,
`linuxX64`, and `mingwX64`, so multi-target shared modules resolve it from `commonMain`
without platform guards.

### Compose Multiplatform occlusion

Compose renders into a single native view, so per-View occlusion can't see individual
composables. The `uxcam-compose` module (auto-installed for Compose consumers) provides:

```kotlin
Text("secret", modifier = Modifier.uxcamOcclude("payment-card"))
```

On Android the complete set of live node bounds is sent through the SDK's public frame-scoped
occlusion API; on iOS it is forwarded through the SDK's identity-based rect occlusion API.

## Project structure

```
uxcam-kmp/        Kotlin Multiplatform library (Android + iOS + no-op targets)
uxcam-compose/    Modifier.uxcamOcclude for Compose Multiplatform
gradle-plugin/    the com.uxcam.kmp Gradle plugin
build-logic/      shared publishing convention (Maven Central Portal)
androidApp/       Android (Jetpack Compose) sample
examples/shared/  shared KMP module sample
iosApp/           iOS (SwiftUI) sample
```

## Building from source

```bash
./gradlew :androidApp:assembleDebug     # Android sample
./scripts/build-ios.sh                  # iOS sample (builds the XCFramework, then the app)
```

Set your UXCam app key in the sample sources before running.

## Requirements

- JDK 17 and the Android SDK (compileSdk 35)
- Kotlin 2.4.0, iOS 15+, and Xcode 26+
- `xcodegen` (for the iOS sample project)

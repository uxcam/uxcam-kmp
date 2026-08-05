# UXCam Kotlin Multiplatform

UXCam session recording and product analytics for Kotlin Multiplatform applications. The shared
Kotlin API uses the native UXCam SDK on Android and iOS.

## Install

Apply the UXCam plugin to your shared module:

```kotlin
plugins {
    id("com.uxcam.kmp") version "<version>"
}
```

The plugin adds `uxcam-kmp` to `commonMain` and adds `uxcam-compose` when Compose is detected.
Android receives the native UXCam SDK transitively.

For multi-module projects, declare the Kotlin and Android plugins once in the root build with
`apply false`.

### Link the iOS SDK

The native UXCam iOS SDK must be linked by the final iOS application:

- **Swift Package Manager:** add `https://github.com/uxcam/uxcam-ios` in Xcode and use a static
  Kotlin framework.
- **CocoaPods:** when the shared module uses `kotlin("native.cocoapods")`, the UXCam plugin adds
  the native pod automatically.

### Install without the Gradle plugin

You can add the shared library directly, but must configure iOS linking yourself:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.uxcam:uxcam-kmp:<version>")
        }
    }
}
```

### Native iOS application

Add the prebuilt package in Xcode, then `import UXCamKMP`:

```text
https://github.com/uxcam/uxcam-kmp
```

## Start UXCam

```kotlin
import com.uxcam.kmp.KMPUXCamBlur
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.uxcamConfiguration

UXCamKMP.startWithConfiguration(
    uxcamConfiguration("YOUR_UXCAM_APP_KEY") {
        enableCrashHandling = true
        occlusions = listOf(
            KMPUXCamBlur(screens = listOf("PaymentScreen")),
        )
    },
)

UXCamKMP.tagScreenName("Home")
UXCamKMP.logEvent("checkout_started", mapOf("cart_size" to 3))
UXCamKMP.setUserIdentity("user-42")
```

Screen names must be tagged explicitly with `tagScreenName`.

The shared API also supports session control, user and session properties, screen-name ignore
lists, bug and exception reporting, privacy occlusion, recording controls, consent management,
verification callbacks, upload status, and dashboard URLs.

## Compose occlusion

For Compose Multiplatform, apply `uxcamOcclude` to sensitive content:

```kotlin
Text(
    text = "Sensitive content",
    modifier = Modifier.uxcamOcclude("payment-card"),
)
```

Startup occlusion rules are configured through `uxcamConfiguration`. Runtime blur and overlay
rules are additive until `UXCamKMP.removeOcclusion()` is called.

## Supported platforms

- Android 24+
- iOS 15+
- No-op targets for JVM, JavaScript, WebAssembly, Linux, and Windows

## Requirements

- Kotlin 2.4.0+
- JDK 17
- Android compile SDK 35+
- Xcode 26+

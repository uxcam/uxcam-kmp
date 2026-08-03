package com.uxcam.kmp.sample

import android.app.Application
import com.uxcam.kmp.KMPUXCamBlur
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.sample.nav.Destination
import com.uxcam.kmp.uxcamConfiguration

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UXCamKMP.startWithConfiguration(
            uxcamConfiguration(getString(R.string.uxcam_app_key)) {
                enableAutomaticScreenNameTagging = true
                enableCrashHandling = true
                enableIntegrationLogging = true
                // Startup occlusion is the only place iOS honours `excludeMentionedScreens`, so
                // screen-scoped rules that must last the whole session belong here rather than
                // in a runtime applyBlurOcclusion call.
                occlusions = listOf(
                    KMPUXCamBlur(
                        blurRadius = 18,
                        screens = listOf(Destination.SensitiveForm.screenName),
                    ),
                )
            }
        )
        SampleLog.add("startWithConfiguration(…) — blur rule on \"Sensitive Form\"")
    }
}

package com.uxcam.kmp.sample.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.resumeShortBreakForAnotherApp
import com.uxcam.kmp.sample.SampleLog
import com.uxcam.kmp.sample.nav.Destination
import com.uxcam.kmp.sample.ui.Action
import com.uxcam.kmp.sample.ui.ScreenScaffold
import com.uxcam.kmp.sample.ui.Section

@Composable
fun HomeScreen(navController: NavHostController) {
    var status by remember { mutableStateOf("Tap “Refresh” to read the SDK state.") }

    fun refresh() {
        status = buildString {
            appendLine("sdk            = ${UXCamKMP.getSdkVersionInfo()}")
            appendLine("isRecording    = ${UXCamKMP.isRecording()}")
            appendLine("optedIn        = ${UXCamKMP.optInOverallStatus()}")
            appendLine("videoOptedIn   = ${UXCamKMP.optInVideoRecordingStatus()}")
            appendLine("multiSession   = ${UXCamKMP.getMultiSessionRecord()}")
            appendLine("pendingUploads = ${UXCamKMP.pendingSessionCount()}")
            appendLine("session URL    = ${UXCamKMP.urlForCurrentSession() ?: "n/a"}")
            append("user URL       = ${UXCamKMP.urlForCurrentUser() ?: "n/a"}")
        }
    }

    ScreenScaffold {
        Text("UXCam KMP test harness", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Five tabs cover the whole shared API. Every call is echoed to the console on the " +
                "Debug tab, and each screen reports its own UXCam screen name.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Section("SDK state") {
            Action("Refresh") { refresh() }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }

        Section("Session lifecycle") {
            Action("startNewSession()") {
                UXCamKMP.startNewSession()
                SampleLog.add("startNewSession()")
            }
            Action("stopSessionAndUploadData()") {
                UXCamKMP.stopSessionAndUploadData()
                SampleLog.add("stopSessionAndUploadData()")
            }
            Action("stopSessionAndUploadData { callback }") {
                UXCamKMP.stopSessionAndUploadData {
                    SampleLog.add("stopSessionAndUploadData → session stopped")
                }
                SampleLog.add("stopSessionAndUploadData(callback)")
            }
            Action("cancelCurrentSession()") {
                UXCamKMP.cancelCurrentSession()
                SampleLog.add("cancelCurrentSession()")
            }
        }

        Section("Recording control") {
            Action("pauseScreenRecording()") {
                UXCamKMP.pauseScreenRecording()
                SampleLog.add("pauseScreenRecording()")
            }
            Action("resumeScreenRecording()") {
                UXCamKMP.resumeScreenRecording()
                SampleLog.add("resumeScreenRecording()")
            }
        }

        Section(
            "Short break",
            "Keeps the session alive while another app is in the foreground.",
        ) {
            Action("allowShortBreakForAnotherApp()") {
                UXCamKMP.allowShortBreakForAnotherApp()
                SampleLog.add("allowShortBreakForAnotherApp()")
            }
            Action("allowShortBreakForAnotherApp(continueSession = true)") {
                UXCamKMP.allowShortBreakForAnotherApp(true)
                SampleLog.add("allowShortBreakForAnotherApp(true)")
            }
            Action("allowShortBreakForAnotherApp(millis = 5000)") {
                UXCamKMP.allowShortBreakForAnotherApp(5_000)
                SampleLog.add("allowShortBreakForAnotherApp(5000)")
            }
            Action("resumeShortBreakForAnotherApp()  — Android only") {
                UXCamKMP.resumeShortBreakForAnotherApp()
                SampleLog.add("resumeShortBreakForAnotherApp()")
            }
        }

        Section("Jump to a flow") {
            Action("Shop → product → checkout") {
                navController.navigate(Destination.Shop.route)
            }
            Action("Privacy scenarios") {
                navController.navigate(Destination.Privacy.route)
            }
        }
    }
}

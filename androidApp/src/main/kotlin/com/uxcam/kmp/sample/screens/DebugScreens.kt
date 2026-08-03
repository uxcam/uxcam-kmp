package com.uxcam.kmp.sample.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.sample.SampleLog
import com.uxcam.kmp.sample.nav.Destination
import com.uxcam.kmp.sample.ui.Action
import com.uxcam.kmp.sample.ui.ScreenScaffold
import com.uxcam.kmp.sample.ui.Section
import com.uxcam.kmp.sample.ui.ToggleRow
import com.uxcam.kmp.setAutomaticScreenNameTagging
import com.uxcam.kmp.setImprovedScreenCaptureEnabled

@Composable
fun DebugScreen(navController: NavHostController) {
    var status by remember { mutableStateOf("") }
    var multiSession by remember { mutableStateOf(UXCamKMP.getMultiSessionRecord()) }
    var crashDisabled by remember { mutableStateOf(false) }
    var autoTagging by remember { mutableStateOf(true) }
    var improvedCapture by remember { mutableStateOf(false) }

    ScreenScaffold {
        Section("Bug & exception reporting") {
            Action("reportBugEvent(name)") {
                UXCamKMP.reportBugEvent("sample_bug")
                SampleLog.add("reportBugEvent(\"sample_bug\")")
            }
            Action("reportBugEvent(name, properties)") {
                UXCamKMP.reportBugEvent("sample_bug", mapOf("severity" to "low", "retry" to 2))
                SampleLog.add("reportBugEvent(\"sample_bug\", 2 properties)")
            }
            Action("reportBugEventWithJson(name, json)") {
                UXCamKMP.reportBugEventWithJson("sample_bug", """{"severity":"low"}""")
                SampleLog.add("reportBugEventWithJson(\"sample_bug\")")
            }
            Action("reportExceptionEvent(throwable)") {
                UXCamKMP.reportExceptionEvent(IllegalStateException("sample handled exception"))
                SampleLog.add("reportExceptionEvent(IllegalStateException)")
            }
            Action("reportExceptionEvent(throwable, properties)") {
                UXCamKMP.reportExceptionEvent(
                    IllegalStateException("sample handled exception"),
                    mapOf("handled" to true),
                )
                SampleLog.add("reportExceptionEvent(IllegalStateException, 1 property)")
            }
        }

        Section("Screen-name ignore list") {
            Action("addScreenNameToIgnore(\"Checkout\")") {
                UXCamKMP.addScreenNameToIgnore(Destination.Checkout.screenName)
                SampleLog.add("addScreenNameToIgnore(\"Checkout\")")
            }
            Action("addScreenNamesToIgnore([\"Profile\", \"Edit Profile\"])") {
                UXCamKMP.addScreenNamesToIgnore(
                    listOf(Destination.Profile.screenName, Destination.EditProfile.screenName),
                )
                SampleLog.add("addScreenNamesToIgnore([Profile, Edit Profile])")
            }
            Action("removeScreenNameToIgnore(\"Checkout\")") {
                UXCamKMP.removeScreenNameToIgnore(Destination.Checkout.screenName)
                SampleLog.add("removeScreenNameToIgnore(\"Checkout\")")
            }
            Action("removeScreenNamesToIgnore([\"Profile\", \"Edit Profile\"])") {
                UXCamKMP.removeScreenNamesToIgnore(
                    listOf(Destination.Profile.screenName, Destination.EditProfile.screenName),
                )
                SampleLog.add("removeScreenNamesToIgnore([Profile, Edit Profile])")
            }
            Action("removeAllScreenNamesToIgnore()") {
                UXCamKMP.removeAllScreenNamesToIgnore()
                SampleLog.add("removeAllScreenNamesToIgnore()")
            }
            Action("screenNamesBeingIgnored()") {
                val ignored = UXCamKMP.screenNamesBeingIgnored()
                status = "ignored = $ignored"
                SampleLog.add("screenNamesBeingIgnored() = $ignored")
            }
        }

        Section("SDK switches") {
            ToggleRow("setMultiSessionRecord", multiSession) {
                multiSession = it
                UXCamKMP.setMultiSessionRecord(it)
                SampleLog.add("setMultiSessionRecord($it)")
            }
            ToggleRow("disableCrashHandling", crashDisabled) {
                crashDisabled = it
                UXCamKMP.disableCrashHandling(it)
                SampleLog.add("disableCrashHandling($it)")
            }
            ToggleRow("setAutomaticScreenNameTagging  — Android only", autoTagging) {
                autoTagging = it
                UXCamKMP.setAutomaticScreenNameTagging(it)
                SampleLog.add("setAutomaticScreenNameTagging($it)")
            }
            ToggleRow("setImprovedScreenCaptureEnabled  — Android only", improvedCapture) {
                improvedCapture = it
                UXCamKMP.setImprovedScreenCaptureEnabled(it)
                SampleLog.add("setImprovedScreenCaptureEnabled($it)")
            }
        }

        Section("Verification & uploads") {
            Action("addVerificationListener()") {
                UXCamKMP.addVerificationListener(
                    onSuccess = { SampleLog.add("verification → success") },
                    onFailure = { message -> SampleLog.add("verification → failed: $message") },
                )
                SampleLog.add("addVerificationListener() registered")
            }
            Action("pendingSessionCount()") {
                status = "pendingSessionCount = ${UXCamKMP.pendingSessionCount()}"
                SampleLog.add(status)
            }
            Action("pendingUploads { callback }") {
                UXCamKMP.pendingUploads { count -> SampleLog.add("pendingUploads → $count") }
                SampleLog.add("pendingUploads(callback)")
            }
            Action("deletePendingUploads()") {
                UXCamKMP.deletePendingUploads()
                SampleLog.add("deletePendingUploads()")
            }
        }

        Section("Diagnostics") {
            Action("getSdkVersionInfo() & session URLs") {
                status = buildString {
                    appendLine("sdk     = ${UXCamKMP.getSdkVersionInfo()}")
                    appendLine("session = ${UXCamKMP.urlForCurrentSession() ?: "n/a"}")
                    append("user    = ${UXCamKMP.urlForCurrentUser() ?: "n/a"}")
                }
                SampleLog.add("getSdkVersionInfo() = ${UXCamKMP.getSdkVersionInfo()}")
            }
            if (status.isNotEmpty()) {
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
            Action("Open console") { navController.navigate(Destination.Console.route) }
        }
    }
}

/** Live view of every UXCam call the sample has made, newest first. */
@Composable
fun ConsoleScreen() {
    ScreenScaffold {
        Section("Console", "${SampleLog.entries.size} entries — newest first.") {
            Action("Clear") { SampleLog.clear() }
            HorizontalDivider()
            if (SampleLog.entries.isEmpty()) {
                Text("No calls recorded yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SampleLog.entries.forEach { entry ->
                        Text(entry, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

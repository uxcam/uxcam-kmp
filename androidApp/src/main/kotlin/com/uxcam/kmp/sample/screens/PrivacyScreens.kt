package com.uxcam.kmp.sample.screens

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.uxcam.kmp.BlurType
import com.uxcam.kmp.KMPUXCamBlur
import com.uxcam.kmp.KMPUXCamOverlay
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.occludeSensitiveView
import com.uxcam.kmp.occludeSensitiveViewWithoutGesture
import com.uxcam.kmp.sample.SampleLog
import com.uxcam.kmp.sample.nav.Destination
import com.uxcam.kmp.sample.ui.Action
import com.uxcam.kmp.sample.ui.ScreenScaffold
import com.uxcam.kmp.sample.ui.Section
import com.uxcam.kmp.sample.ui.ToggleRow
import com.uxcam.kmp.unOccludeSensitiveView
import com.uxcam.kmp.uxcamOcclude

@Composable
fun PrivacyScreen(navController: NavHostController) {
    var hideScreen by remember { mutableStateOf(false) }
    var occludeFields by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var sensitiveView by remember { mutableStateOf<TextView?>(null) }

    ScreenScaffold {
        Section("Whole-screen occlusion") {
            ToggleRow("occludeSensitiveScreen", hideScreen) {
                hideScreen = it
                UXCamKMP.occludeSensitiveScreen(it)
                SampleLog.add("occludeSensitiveScreen($it)")
            }
            Action("occludeSensitiveScreen(true, withoutGesture = true)") {
                UXCamKMP.occludeSensitiveScreen(true, withoutGesture = true)
                SampleLog.add("occludeSensitiveScreen(true, withoutGesture = true)")
            }
            ToggleRow("occludeAllTextFields", occludeFields) {
                occludeFields = it
                UXCamKMP.occludeAllTextFields(it)
                SampleLog.add("occludeAllTextFields($it)")
            }
        }

        Section("Blur rules", "Radius and type are honoured on iOS; Android uses radius only.") {
            Action("Gaussian blur, radius 20") {
                UXCamKMP.applyBlurOcclusion(KMPUXCamBlur(blurRadius = 20))
                SampleLog.add("applyBlurOcclusion(Gaussian, 20)")
            }
            Action("Box blur, radius 10") {
                UXCamKMP.applyBlurOcclusion(
                    KMPUXCamBlur(blurRadius = 10, blurType = BlurType.Box),
                )
                SampleLog.add("applyBlurOcclusion(Box, 10)")
            }
            Action("Bokeh blur, gestures kept") {
                UXCamKMP.applyBlurOcclusion(
                    KMPUXCamBlur(blurType = BlurType.Bokeh, hideGestures = false),
                )
                SampleLog.add("applyBlurOcclusion(Bokeh, hideGestures = false)")
            }
            Action("Blur only the Checkout screen") {
                UXCamKMP.applyBlurOcclusion(
                    KMPUXCamBlur(screens = listOf(Destination.Checkout.screenName)),
                )
                SampleLog.add("applyBlurOcclusion(screens = [Checkout])")
            }
        }

        Section("Overlay rules") {
            Action("Opaque overlay everywhere") {
                UXCamKMP.applyOverlayOcclusion(KMPUXCamOverlay(color = 0xFF222222.toInt()))
                SampleLog.add("applyOverlayOcclusion(#222222)")
            }
            Action("Overlay everywhere except Home") {
                UXCamKMP.applyOverlayOcclusion(
                    KMPUXCamOverlay(
                        screens = listOf(Destination.Home.screenName),
                        excludeMentionedScreens = true,
                    ),
                )
                SampleLog.add("applyOverlayOcclusion(exclude = [Home])")
            }
            Action("removeOcclusion()  — clears every rule above") {
                UXCamKMP.removeOcclusion()
                SampleLog.add("removeOcclusion()")
            }
        }

        Section("Per-composable occlusion", "uxcam-compose Modifier.uxcamOcclude") {
            Text(
                "🔒 Occluded composable (identifier: privacy-secret)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .uxcamOcclude("privacy-secret"),
            )
            Action("Open dialog with occluded content") { showDialog = true }
        }

        Section("Per-View occlusion", "Android-only extensions for android.view.View") {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    TextView(context).apply {
                        text = "🔒 Sensitive TextView (occludeSensitiveView)"
                        UXCamKMP.occludeSensitiveView(this)
                        sensitiveView = this
                    }
                },
            )
            Action("occludeSensitiveViewWithoutGesture(view)") {
                sensitiveView?.let {
                    UXCamKMP.occludeSensitiveViewWithoutGesture(it)
                    SampleLog.add("occludeSensitiveViewWithoutGesture(view)")
                }
            }
            Action("unOccludeSensitiveView(view)") {
                sensitiveView?.let {
                    UXCamKMP.unOccludeSensitiveView(it)
                    SampleLog.add("unOccludeSensitiveView(view)")
                }
            }
        }

        Section("More") {
            Action("Open sensitive form") {
                navController.navigate(Destination.SensitiveForm.route)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Dialog occlusion") },
            text = {
                Text(
                    "🔒 Occluded inside a dialog — uxcamOcclude(isInDialog = true) maps the " +
                        "bounds to the dialog window instead of the activity window.",
                    modifier = Modifier.uxcamOcclude("dialog-secret", isInDialog = true),
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Close") }
            },
        )
    }
}

/** A form where `occludeAllTextFields` and per-field occlusion can be compared side by side. */
@Composable
fun SensitiveFormScreen() {
    var name by remember { mutableStateOf("Ada Lovelace") }
    var ssn by remember { mutableStateOf("123-45-6789") }
    var notes by remember { mutableStateOf("Not sensitive") }

    ScreenScaffold {
        Text(
            "The SSN field is occluded per-composable; the others are only hidden when " +
                "occludeAllTextFields is on.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Section("Form") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ssn,
                onValueChange = { ssn = it },
                label = { Text("SSN (occluded)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .uxcamOcclude("form-ssn"),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )
            Action("Submit") {
                UXCamKMP.logEvent("form_submitted", mapOf("fields" to 3))
                SampleLog.add("logEvent(\"form_submitted\")")
            }
        }
    }
}

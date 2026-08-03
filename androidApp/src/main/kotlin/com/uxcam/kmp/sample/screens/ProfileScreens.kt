package com.uxcam.kmp.sample.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.uxcam.kmp.UXCamKMP
import com.uxcam.kmp.markSessionAsFavorite
import com.uxcam.kmp.sample.SampleLog
import com.uxcam.kmp.sample.nav.Destination
import com.uxcam.kmp.sample.ui.Action
import com.uxcam.kmp.sample.ui.ScreenScaffold
import com.uxcam.kmp.sample.ui.Section
import com.uxcam.kmp.uxcamOcclude

@Composable
fun ProfileScreen(navController: NavHostController) {
    var userId by remember { mutableStateOf("user-42") }
    var status by remember { mutableStateOf("") }

    ScreenScaffold {
        Section("Identity") {
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("User identity") },
                modifier = Modifier.fillMaxWidth(),
            )
            Action("setUserIdentity(id)") {
                UXCamKMP.setUserIdentity(userId)
                SampleLog.add("setUserIdentity(\"$userId\")")
            }
            Action("setPushNotificationToken(token)") {
                UXCamKMP.setPushNotificationToken("sample-push-token")
                SampleLog.add("setPushNotificationToken(…)")
            }
        }

        Section("User properties", "One call per supported value type.") {
            Action("Set String / Int / Float / Boolean properties") {
                UXCamKMP.setUserProperty("plan", "premium")
                UXCamKMP.setUserProperty("age", 29)
                UXCamKMP.setUserProperty("score", 0.87f)
                UXCamKMP.setUserProperty("beta_tester", true)
                SampleLog.add("setUserProperty × 4 (String, Int, Float, Boolean)")
            }
        }

        Section("Session properties") {
            Action("Set String / Int / Float / Boolean properties") {
                UXCamKMP.setSessionProperty("ab_bucket", "checkout_v2")
                UXCamKMP.setSessionProperty("cart_value", 42)
                UXCamKMP.setSessionProperty("conversion_rate", 0.31f)
                UXCamKMP.setSessionProperty("is_returning", true)
                SampleLog.add("setSessionProperty × 4 (String, Int, Float, Boolean)")
            }
        }

        Section("Consent") {
            Action("optInOverall()") {
                UXCamKMP.optInOverall()
                SampleLog.add("optInOverall()")
            }
            Action("optOutOverall()") {
                UXCamKMP.optOutOverall()
                SampleLog.add("optOutOverall()")
            }
            Action("optIntoVideoRecording()") {
                UXCamKMP.optIntoVideoRecording()
                SampleLog.add("optIntoVideoRecording()")
            }
            Action("optOutOfVideoRecording()") {
                UXCamKMP.optOutOfVideoRecording()
                SampleLog.add("optOutOfVideoRecording()")
            }
            Action("Read consent status") {
                status = "optedIn=${UXCamKMP.optInOverallStatus()}, " +
                    "video=${UXCamKMP.optInVideoRecordingStatus()}"
                SampleLog.add(status)
            }
            if (status.isNotEmpty()) {
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }

        Section("Android-only") {
            Action("markSessionAsFavorite()") {
                UXCamKMP.markSessionAsFavorite()
                SampleLog.add("markSessionAsFavorite()")
            }
        }

        Section("More") {
            Action("Edit profile") { navController.navigate(Destination.EditProfile.route) }
        }
    }
}

@Composable
fun EditProfileScreen() {
    var email by remember { mutableStateOf("ada@example.com") }
    var phone by remember { mutableStateOf("+1 555 0100") }

    ScreenScaffold {
        Text(
            "A second page inside the Profile tab — useful for checking that back navigation " +
                "re-tags the parent screen name.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Section("Contact details") {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (occluded)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .uxcamOcclude("profile-email"),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (occluded)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .uxcamOcclude("profile-phone"),
            )
            Action("Save") {
                UXCamKMP.logEvent("profile_updated", mapOf("fields" to 2))
                SampleLog.add("logEvent(\"profile_updated\")")
            }
        }
    }
}

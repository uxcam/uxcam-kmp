import SwiftUI
import Shared

private let screenName = "UXCam KMP Sample - Home (iOS)"

struct ContentView: View {
    @State private var status = ""
    @State private var eventName = "checkout_started"
    @State private var userId = "user-42"

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Calls go through the shared KMP module (SharedAnalytics) → uxcam-kmp.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Section("Session") {
                    Button("Refresh status") {
                        let recording = SharedAnalytics.shared.isRecording()
                        let session = SharedAnalytics.shared.sessionUrl() ?? "n/a"
                        status = "isRecording=\(recording)\nsession=\(session)"
                    }
                    if !status.isEmpty {
                        Text(status).font(.caption).foregroundStyle(.secondary)
                    }
                }

                Section("Events") {
                    TextField("Event name", text: $eventName)
                    Button("Log event") { SharedAnalytics.shared.logEvent(name: eventName) }
                    Button("Log event with properties") {
                        SharedAnalytics.shared.logEvent(
                            name: eventName,
                            properties: ["source": "ios-sample", "cart_size": 3]
                        )
                    }
                }

                Section("User & session properties") {
                    TextField("User identity", text: $userId)
                    Button("Set user identity") { SharedAnalytics.shared.setUser(id: userId) }
                    Button("Set session property") {
                        SharedAnalytics.shared.setSessionProperty(name: "ab_bucket", value: "checkout_v2")
                    }
                }

                Section("Bug reporting") {
                    Button("Report bug event") { SharedAnalytics.shared.reportBug(name: "sample_bug") }
                }

                Section("Occlusion") {
                    Button("Blur whole screen") { SharedAnalytics.shared.blurScreen() }
                    Button("Overlay whole screen") { SharedAnalytics.shared.overlayScreen() }
                    Button("Remove occlusion") { SharedAnalytics.shared.clearOcclusion() }
                }

                Section("Privacy & diagnostics") {
                    Button("Opt out") { SharedAnalytics.shared.optOut() }
                    Button("Opt in") { SharedAnalytics.shared.optIn() }
                    Button("SDK info") {
                        let optedIn = SharedAnalytics.shared.optInStatus()
                        let pending = SharedAnalytics.shared.pendingSessions()
                        let version = SharedAnalytics.shared.sdkVersion()
                        status = "sdk=\(version)\npendingSessions=\(pending)\noptedIn=\(optedIn)"
                    }
                }
            }
            .navigationTitle("uxcam-kmp")
        }
        .onAppear {
            SharedAnalytics.shared.tagScreen(name: screenName)
            SharedAnalytics.shared.onVerification { ok, message in
                status = ok.boolValue ? "verification: success" : "verification failed: \(message ?? "")"
            }
        }
    }
}

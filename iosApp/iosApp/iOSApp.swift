import SwiftUI
import Shared

enum AppConfig {
    static let uxcamAppKey = "YOUR_UXCAM_APP_KEY"
}

@main
struct iOSApp: App {
    init() {
        SharedAnalytics.shared.start(appKey: AppConfig.uxcamAppKey)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

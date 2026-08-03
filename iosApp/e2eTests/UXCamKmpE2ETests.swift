import XCTest

/// Drives the iOS sample through the shared-API scenarios so the resulting UXCam session can be
/// verified on the backend. Mirrors the Android sample sweep: session state, screen tagging,
/// events, identity/properties, bug reporting, additive occlusion, and consent.
///
/// Each step sleeps briefly so the recorded replay has distinguishable frames per scenario.
final class UXCamKmpE2ETests: XCTestCase {

    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    /// The sample is a scrolling SwiftUI Form, so a row can exist off-screen (or not yet exist at
    /// all). Scroll back to the top, then page down until the row is on-screen and hittable.
    private func scrollTo(_ label: String, maxSwipes: Int = 10) -> XCUIElement {
        let button = app.buttons[label]
        if button.exists && button.isHittable { return button }

        for _ in 0..<4 where !(button.exists && button.isHittable) {
            app.swipeDown()
        }
        var swipes = 0
        while !(button.exists && button.isHittable) && swipes < maxSwipes {
            app.swipeUp()
            swipes += 1
        }
        return button
    }

    private func tap(_ label: String, settle: TimeInterval = 2.0, file: StaticString = #filePath, line: UInt = #line) {
        let button = scrollTo(label)
        XCTAssertTrue(
            button.exists && button.isHittable,
            "missing button: \(label)",
            file: file,
            line: line
        )
        button.tap()
        Thread.sleep(forTimeInterval: settle)
    }

    /// Reads the status caption the sample renders. Both "Refresh status" and "SDK info" write to
    /// the same `status` state, which is rendered in the *Session* section at the top of the Form —
    /// while "SDK info" itself sits at the bottom. Scroll back up first or the caption is outside
    /// the accessibility tree and reads as empty.
    private func statusText() -> String {
        func collect() -> String {
            app.staticTexts.allElementsBoundByIndex
                .map { $0.label }
                .joined(separator: "\n")
        }
        func rendered(_ s: String) -> Bool {
            s.contains("isRecording") || s.contains("sdk=")
        }

        var text = collect()
        // Only scroll when the caption really isn't in the tree — swiping unconditionally can
        // scroll a Form that is already at the top and costs a retry for nothing.
        for _ in 0..<4 where !rendered(text) {
            app.swipeDown()
            Thread.sleep(forTimeInterval: 0.4)
            text = collect()
        }
        return text
    }

    func testFullScenarioSweep() throws {
        // --- 1. Session state: SDK started, verification listener registered in onAppear ---
        tap("Refresh status")
        let afterRefresh = statusText()
        XCTAssertTrue(afterRefresh.contains("isRecording"), "status never rendered:\n\(afterRefresh)")
        print("E2E-STATUS-SESSION >>>\n\(afterRefresh)\n<<<")

        // --- 2. Consent first. optOutOverall() ends the current session and optInOverall() starts
        // a fresh one, so anything logged before this point lands in a session that is discarded.
        // Running consent up front keeps every later scenario inside the session that uploads.
        tap("Opt out", settle: 4.0)
        tap("SDK info")
        let optedOut = statusText()
        XCTAssertTrue(optedOut.contains("optedIn=false"), "opt-out not reflected:\n\(optedOut)")
        print("E2E-STATUS-OPTED-OUT >>>\n\(optedOut)\n<<<")

        tap("Opt in", settle: 6.0)
        tap("SDK info")
        let optedIn = statusText()
        XCTAssertTrue(optedIn.contains("optedIn=true"), "opt-in not reflected:\n\(optedIn)")
        print("E2E-STATUS-OPTED-IN >>>\n\(optedIn)\n<<<")

        // --- 3. Events: both logEvent overloads ---
        tap("Log event")
        tap("Log event with properties")

        // --- 3. Identity & session properties ---
        tap("Set user identity")
        tap("Set session property")

        // --- 4. Bug reporting ---
        tap("Report bug event")

        // --- 5. Occlusion — additive semantics.
        // Blur first, then overlay on top: the overlay must NOT clear the blur (Android used to,
        // iOS never did; both are additive now). Give each a long settle so the replay shows it.
        tap("Blur whole screen", settle: 6.0)
        tap("Overlay whole screen", settle: 6.0)
        // removeOcclusion() must clear BOTH runtime rules.
        tap("Remove occlusion", settle: 6.0)

        // Re-apply a single rule then clear it, to bracket a clean visible window in the replay.
        tap("Blur whole screen", settle: 5.0)
        tap("Remove occlusion", settle: 5.0)

        // --- 6. Diagnostics: SDK version, pending sessions, opt-in status ---
        tap("SDK info")
        let diagnostics = statusText()
        XCTAssertTrue(diagnostics.contains("sdk="), "SDK info never rendered:\n\(diagnostics)")
        print("E2E-STATUS-DIAGNOSTICS >>>\n\(diagnostics)\n<<<")

        // --- 8. Final state + session URL for backend lookup ---
        tap("Refresh status")
        print("E2E-STATUS-FINAL >>>\n\(statusText())\n<<<")

        // Let the tail of the session record before the harness tears the app down.
        Thread.sleep(forTimeInterval: 5.0)

        // The iOS SDK finalizes and uploads a session when the app enters the background. If the
        // test harness just kills the process, the upload record keeps `artifacts = ()` and the
        // session never reaches the backend — so background the app explicitly and give the
        // background URLSession time to flush before teardown.
        XCUIDevice.shared.press(.home)
        Thread.sleep(forTimeInterval: 25.0)
    }
}

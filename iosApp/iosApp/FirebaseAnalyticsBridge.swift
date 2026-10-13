import FirebaseAnalytics
import FirebaseCore
import Foundation
import Shared

/// Firebase Analytics for the shared Compose UI. Firebase is configured from GoogleService-Info.plist
/// (downloaded from the Firebase console); without that file the app runs without analytics.
final class FirebaseAnalyticsBridge: NSObject, IosAnalytics {
    /// Nil when GoogleService-Info.plist is not bundled.
    static let shared: FirebaseAnalyticsBridge? = {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return nil }
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        return FirebaseAnalyticsBridge()
    }()

    /// Configures Firebase at launch, before other Google SDKs look for it.
    static func configureAtLaunch() {
        _ = shared
    }

    func logScreenView(name: String) {
        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: name,
            AnalyticsParameterScreenClass: name,
        ])
    }

    func logEvent(name: String) {
        Analytics.logEvent(name, parameters: nil)
    }
}

import SwiftUI

@main
struct iOSApp: App {
    init() {
        FirebaseAnalyticsBridge.configureAtLaunch()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

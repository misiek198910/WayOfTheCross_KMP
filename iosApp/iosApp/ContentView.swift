import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            ads: AdMobAds.shared,
            analytics: FirebaseAnalyticsBridge.shared,
            // The subscription is Android only for now; on iOS a rewarded ad turns ads off for 24 hours.
            subscriptions: nil
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

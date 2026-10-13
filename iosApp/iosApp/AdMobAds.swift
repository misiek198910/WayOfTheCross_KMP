import AppTrackingTransparency
import GoogleMobileAds
import Shared
import UIKit
import UserMessagingPlatform

/// Google Mobile Ads for the shared Compose UI, mirroring the Android AdsManager:
/// consent (UMP) -> SDK start -> app open ad, plus anchored banners.
final class AdMobAds: NSObject, IosAds {
    private struct UnitIDs {
        let banner: String
        let appOpen: String
        let rewarded: String?
    }

    #if DEBUG
    // Google's official test units, so development never produces invalid traffic on the real account.
    private static let unitIDs: UnitIDs? = UnitIDs(
        banner: "ca-app-pub-3940256099942544/2435281174",
        appOpen: "ca-app-pub-3940256099942544/5575463023",
        rewarded: "ca-app-pub-3940256099942544/1712485313"
    )
    #else
    // Real units come from the git-ignored Configuration/Secrets.xcconfig through Info.plist;
    // without them release builds run without ads.
    private static let unitIDs: UnitIDs? = {
        func unit(_ key: String) -> String? {
            (Bundle.main.object(forInfoDictionaryKey: key) as? String).flatMap { $0.isEmpty ? nil : $0 }
        }
        guard let banner = unit("DKAdBannerUnitID"), let appOpen = unit("DKAdAppOpenUnitID") else { return nil }
        return UnitIDs(banner: banner, appOpen: appOpen, rewarded: unit("DKAdRewardedUnitID"))
    }()
    #endif

    private static let appOpenTimeout: TimeInterval = 5

    /// Nil when no ad units are configured for this build.
    static let shared: AdMobAds? = unitIDs.map(AdMobAds.init)

    private let ids: UnitIDs
    // The ad holds its delegate weakly; both are kept here until the ad is dismissed.
    private var appOpenAd: AppOpenAd?
    private var appOpenDelegate: AppOpenDelegate?
    private var rewardedAd: RewardedAd?
    private var rewardedDelegate: RewardedDelegate?
    private let bannerDelegate = BannerDelegate()
    private var started = false
    private var adsAllowed = false

    private init(ids: UnitIDs) {
        self.ids = ids
    }

    func start(onReady: @escaping (KotlinBoolean) -> Void) {
        let finish = OnceCallback { [weak self] in onReady(KotlinBoolean(bool: self?.adsAllowed ?? false)) }
        if started {
            finish.invoke()
            return
        }
        started = true

        let consent = ConsentInformation.shared
        let parameters = RequestParameters()
        #if DEBUG
        // Launch with `-adsOutsideEEA` to test the ad flow without the consent form (UMP then treats the
        // device as outside any regulated region). Handy on simulators, where nothing can tap the form.
        if ProcessInfo.processInfo.arguments.contains("-adsOutsideEEA") {
            consent.reset()
            let debug = DebugSettings()
            debug.geography = .other
            parameters.debugSettings = debug
        }
        #endif
        consent.requestConsentInfoUpdate(with: parameters) { [weak self] error in
            if let error { print("Consent update: \(error.localizedDescription)") }
            ConsentForm.loadAndPresentIfRequired(from: Self.topViewController()) { formError in
                if let formError { print("Consent form: \(formError.localizedDescription)") }
                Self.requestTrackingAuthorizationIfNeeded {
                    guard let self, consent.canRequestAds else { return finish.invoke() }
                    self.initializeAndShowAppOpen(finish)
                }
            }
        }
    }

    var privacyOptionsRequired: Bool {
        ConsentInformation.shared.privacyOptionsRequirementStatus == .required
    }

    func showPrivacyOptions() {
        Task { @MainActor in
            try? await ConsentForm.presentPrivacyOptionsForm(from: Self.topViewController())
        }
    }

    func createBanner(widthPoints: Double, landscape: Bool) -> UIView {
        // Google's recommended anchored adaptive size in both orientations.
        let size = largeAnchoredAdaptiveBanner(width: widthPoints)
        let banner = BannerView(adSize: size)
        banner.adUnitID = ids.banner
        banner.rootViewController = Self.topViewController()
        banner.delegate = bannerDelegate
        banner.load(Request())
        return banner
    }

    var rewardedConfigured: Bool { ids.rewarded != nil }

    func loadRewarded(onLoaded: @escaping (KotlinBoolean) -> Void) {
        guard adsAllowed, let unit = ids.rewarded else { return onLoaded(KotlinBoolean(bool: false)) }
        RewardedAd.load(with: unit, request: Request()) { [weak self] ad, error in
            DispatchQueue.main.async {
                if let error { print("Rewarded ad failed to load: \(error.localizedDescription)") }
                self?.rewardedAd = ad
                onLoaded(KotlinBoolean(bool: ad != nil))
            }
        }
    }

    func showRewarded(onRewarded: @escaping () -> Void, onClosed: @escaping () -> Void) {
        guard let ad = rewardedAd, let root = Self.topViewController() else { return onClosed() }
        rewardedAd = nil
        let delegate = RewardedDelegate { [weak self] in
            self?.rewardedDelegate = nil
            onClosed()
        }
        ad.fullScreenContentDelegate = delegate
        rewardedDelegate = delegate
        ad.present(from: root) { onRewarded() }
    }

    private func initializeAndShowAppOpen(_ finish: OnceCallback) {
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.appOpenTimeout) { finish.timeout() }
        MobileAds.shared.start { [weak self] _ in
            guard let self else { return finish.invoke() }
            self.adsAllowed = true
            AppOpenAd.load(with: self.ids.appOpen, request: Request()) { ad, error in
                DispatchQueue.main.async {
                    guard let ad, error == nil, !finish.done, let root = Self.topViewController() else {
                        if let error { print("App open ad failed to load: \(error.localizedDescription)") }
                        return finish.invoke()
                    }
                    let delegate = AppOpenDelegate(finish: finish)
                    ad.fullScreenContentDelegate = delegate
                    self.appOpenAd = ad
                    self.appOpenDelegate = delegate
                    finish.holdOff()
                    ad.present(from: root)
                }
            }
        }
    }

    /// Apple's tracking prompt (IDFA), asked once after the UMP form; a no-op when already answered,
    /// e.g. when an IDFA explainer message configured in AdMob has shown it already.
    /// The prompt only appears while the app is active; asked earlier, iOS calls back at once and shows it later.
    private static func requestTrackingAuthorizationIfNeeded(_ completion: @escaping () -> Void) {
        guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined else { return completion() }
        guard UIApplication.shared.applicationState == .active else {
            var observer: NSObjectProtocol?
            observer = NotificationCenter.default.addObserver(
                forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main
            ) { _ in
                observer.map(NotificationCenter.default.removeObserver)
                requestTrackingAuthorizationIfNeeded(completion)
            }
            return
        }
        ATTrackingManager.requestTrackingAuthorization { _ in
            DispatchQueue.main.async { completion() }
        }
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

/// Calls the wrapped callback at most once; a showing ad suspends the timeout via `holdOff()`.
private final class OnceCallback {
    private let callback: () -> Void
    private(set) var done = false
    private var adShowing = false

    init(_ callback: @escaping () -> Void) {
        self.callback = callback
    }

    func holdOff() {
        adShowing = true
    }

    func invoke() {
        DispatchQueue.main.async {
            guard !self.done else { return }
            self.done = true
            self.callback()
        }
    }

    /// Timeout entry point: ignored while an ad is on screen, its dismissal finishes instead.
    func timeout() {
        if !adShowing { invoke() }
    }
}

private final class AppOpenDelegate: NSObject, FullScreenContentDelegate {
    private let finish: OnceCallback

    init(finish: OnceCallback) {
        self.finish = finish
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        finish.invoke()
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        finish.invoke()
    }
}

private final class BannerDelegate: NSObject, BannerViewDelegate {
    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        print("Banner ad loaded")
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        print("Banner ad failed to load: \(error.localizedDescription)")
    }
}

private final class RewardedDelegate: NSObject, FullScreenContentDelegate {
    private let closed: () -> Void

    init(closed: @escaping () -> Void) {
        self.closed = closed
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        closed()
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        closed()
    }
}

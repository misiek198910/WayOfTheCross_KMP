package droga_krzyzowa.droga_krzyzowa.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene

/** Ads, analytics and subscriptions are optional Swift bridges; with nil the app simply runs without them. */
class IosPlatformServices(
    private val ads: IosAds?,
    private val analytics: IosAnalytics?,
    private val subscriptions: IosSubscriptions?,
) : PlatformServices {
    private var adsReady by mutableStateOf(false)

    override val supportsStoreLinks: Boolean = false

    override val supportsAdFree: Boolean = subscriptions != null

    override val versionName: String =
        NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    override fun openStorePage(appId: String?) = Unit

    override fun requestReview() {
        val scene = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            ?: return
        @Suppress("DEPRECATION")
        SKStoreReviewController.requestReviewInScene(scene)
    }

    override val updateReady: StateFlow<Boolean> = MutableStateFlow(false)

    override fun completeUpdate() = Unit

    override fun logScreenView(name: String) {
        analytics?.logScreenView(name = name)
    }

    override fun logEvent(name: String) {
        analytics?.logEvent(name = name)
    }

    // News push notifications are Android only for now (Firebase Cloud Messaging).
    override val notificationPromptPending: Boolean = false

    override fun answerNotificationPrompt(accepted: Boolean) = Unit

    private val defaults = NSUserDefaults.standardUserDefaults
    private val scope = MainScope()

    private val adFreeState = MutableStateFlow(
        AdFreeState(active = subscriptions?.cachedActive ?: false, rewardUntilMillis = storedRewardUntil()),
    )
    override val adFree: StateFlow<AdFreeState> = adFreeState

    private fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

    private fun storedRewardUntil(): Long? =
        defaults.doubleForKey(KEY_REWARD_UNTIL).toLong().takeIf { it > nowMillis() }

    private var rewardExpiry: Job? = null

    private fun scheduleRewardExpiry(until: Long) {
        rewardExpiry?.cancel()
        rewardExpiry = scope.launch {
            delay(until - nowMillis())
            defaults.removeObjectForKey(KEY_REWARD_UNTIL)
            adFreeState.update { it.copy(rewardUntilMillis = null) }
        }
    }

    init {
        adFreeState.value.rewardUntilMillis?.let(::scheduleRewardExpiry)
        subscriptions?.start { active, price, period ->
            val plan = if (price != null && period != null) AdFreePlan(IOS_PLAN_ID, price, period) else null
            adFreeState.update { it.copy(active = active, plans = listOfNotNull(plan)) }
        }
    }

    override val supportsRewardedAdFree: Boolean = ads?.rewardedConfigured ?: false

    private val rewardedState = MutableStateFlow(RewardedAdState.Unavailable)
    override val rewardedAd: StateFlow<RewardedAdState> = rewardedState

    override fun loadRewardedAd() {
        if (ads == null || !supportsRewardedAdFree || !adsReady || rewardedState.value != RewardedAdState.Unavailable) return
        rewardedState.value = RewardedAdState.Loading
        ads.loadRewarded { loaded -> rewardedState.value = if (loaded) RewardedAdState.Ready else RewardedAdState.Unavailable }
    }

    override fun showRewardedAd() {
        if (ads == null || rewardedState.value != RewardedAdState.Ready) return
        rewardedState.value = RewardedAdState.Unavailable
        ads.showRewarded(
            onRewarded = {
                val until = nowMillis() + REWARD_MILLIS
                defaults.setDouble(until.toDouble(), KEY_REWARD_UNTIL)
                adFreeState.update { it.copy(rewardUntilMillis = until) }
                scheduleRewardExpiry(until)
            },
            onClosed = { if (adFreeState.value.rewardUntilMillis == null) loadRewardedAd() },
        )
    }

    override fun purchaseAdFree(planId: String) {
        subscriptions?.purchase()
    }

    override fun restorePurchases() {
        subscriptions?.restore()
    }

    override val subscriptionTermsUrl: String = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"

    override fun manageSubscriptions() {
        subscriptions?.manage()
    }

    override val adPrivacyOptionsRequired: Boolean
        get() = ads?.privacyOptionsRequired ?: false

    override fun showAdPrivacyOptions() {
        ads?.showPrivacyOptions()
    }

    override fun prepareAppStart(onReady: () -> Unit) {
        if (ads == null || adFreeState.value.adsHidden) return onReady()
        ads.start { allowed ->
            adsReady = allowed
            onReady()
        }
    }

    @Composable
    override fun AdBanner(modifier: Modifier) {
        val adFreeNow by adFree.collectAsState()
        if (ads != null && adsReady && !adFreeNow.adsHidden) IosBanner(ads, modifier)
    }

    private companion object {
        const val KEY_REWARD_UNTIL = "rewarded_ad_free_until_millis"
        const val REWARD_MILLIS = 24 * 60 * 60 * 1000L
    }
}

private const val IOS_PLAN_ID = "ios"

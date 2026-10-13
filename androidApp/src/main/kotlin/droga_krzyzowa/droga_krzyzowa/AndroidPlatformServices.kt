package droga_krzyzowa.droga_krzyzowa

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import droga_krzyzowa.droga_krzyzowa.ads.AdsManager
import droga_krzyzowa.droga_krzyzowa.ads.BannerAd
import droga_krzyzowa.droga_krzyzowa.ads.RewardedAdFree
import droga_krzyzowa.droga_krzyzowa.billing.AdFreeBilling
import droga_krzyzowa.droga_krzyzowa.platform.AdFreeState
import droga_krzyzowa.droga_krzyzowa.platform.PlatformServices
import droga_krzyzowa.droga_krzyzowa.platform.RewardedAdState
import droga_krzyzowa.droga_krzyzowa.push.NewsNotifications
import droga_krzyzowa.droga_krzyzowa.store.InAppUpdates
import droga_krzyzowa.droga_krzyzowa.store.requestInAppReview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AndroidPlatformServices(private val activity: ComponentActivity) : PlatformServices {

    // Created here because AndroidPlatformServices is built in Activity.onCreate.
    private val inAppUpdates = InAppUpdates(activity)
    private val billing = AdFreeBilling.get(activity).also { billing ->
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = billing.refresh()
        })
    }

    private val notificationPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) NewsNotifications.subscribe(activity)
        }

    init {
        NewsNotifications.createChannel(activity)
        if (NewsNotifications.isPermissionGranted(activity)) NewsNotifications.subscribe(activity)
    }

    override val supportsStoreLinks: Boolean = true

    override val supportsAdFree: Boolean = true

    override val versionName: String = BuildConfig.VERSION_NAME

    override fun openUrl(url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // No browser installed; nothing sensible to do.
        }
    }

    override fun openStorePage(appId: String?) {
        val id = appId ?: activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))
        } catch (_: ActivityNotFoundException) {
            openUrl("https://play.google.com/store/apps/details?id=$id")
        }
    }

    override fun requestReview() {
        activity.requestInAppReview()
    }

    override val updateReady: StateFlow<Boolean> = inAppUpdates.updateReady

    override fun completeUpdate() {
        inAppUpdates.completeUpdate()
    }

    // Null when google-services.json was missing at build time, so Firebase is not initialized.
    private val analytics: FirebaseAnalytics? =
        if (FirebaseApp.getApps(activity).isNotEmpty()) FirebaseAnalytics.getInstance(activity) else null

    override fun logScreenView(name: String) {
        if (BuildConfig.DEBUG) Log.d("Analytics", "screen_view: $name")
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, name)
        })
    }

    override fun logEvent(name: String) {
        if (BuildConfig.DEBUG) Log.d("Analytics", "event: $name")
        analytics?.logEvent(name, null)
    }

    override val notificationPromptPending: Boolean
        get() = !NewsNotifications.isPermissionGranted(activity) && !NewsNotifications.isPromptHandled(activity)

    override fun answerNotificationPrompt(accepted: Boolean) {
        NewsNotifications.markPromptHandled(activity)
        if (accepted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val rewarded = RewardedAdFree.get(activity)

    override val adFree: StateFlow<AdFreeState> =
        combine(billing.state, rewarded.until) { state, until -> state.copy(rewardUntilMillis = until) }
            .stateIn(activity.lifecycleScope, SharingStarted.Eagerly, billing.state.value.copy(rewardUntilMillis = rewarded.until.value))

    // "Watch an ad for 24 h without ads", next to the subscription; only offered when a rewarded unit is configured.
    override val supportsRewardedAdFree: Boolean = BuildConfig.AD_REWARDED_ID.isNotEmpty()

    override val rewardedAd: StateFlow<RewardedAdState> = rewarded.state

    override fun loadRewardedAd() = rewarded.load(activity)

    override fun showRewardedAd() = rewarded.show(activity)

    override fun purchaseAdFree(planId: String) = billing.purchase(activity, planId)

    override fun restorePurchases() = billing.refresh()

    override fun manageSubscriptions() = billing.manage()

    // Google Play's own terms cover the purchase; only the privacy policy is linked.
    override val subscriptionTermsUrl: String? = null

    override val adPrivacyOptionsRequired: Boolean
        get() = UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun showAdPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            error?.let { Log.w("AdPrivacy", "Privacy options form: ${it.message}") }
        }
    }

    override fun prepareAppStart(onReady: () -> Unit) {
        // Subscribers (and a running rewarded ad-free day) skip consent and the app open ad entirely.
        if (adFree.value.adsHidden) {
            onReady()
            inAppUpdates.checkForUpdate()
            return
        }
        // Check for updates only after consent and the app open ad, so the dialogs don't overlap.
        AdsManager.start(activity) {
            onReady()
            inAppUpdates.checkForUpdate()
        }
    }

    @Composable
    override fun AdBanner(modifier: Modifier, header: @Composable () -> Unit) {
        val adFreeState by adFree.collectAsState()
        if (!adFreeState.adsHidden && AdsManager.adsReady) {
            Column(modifier) {
                header()
                BannerAd(Modifier.fillMaxWidth())
            }
        }
    }
}

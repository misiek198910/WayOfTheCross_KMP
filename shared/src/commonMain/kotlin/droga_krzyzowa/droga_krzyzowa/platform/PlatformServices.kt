package droga_krzyzowa.droga_krzyzowa.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

interface PlatformServices {
    val supportsStoreLinks: Boolean

    val versionName: String

    fun openUrl(url: String)

    fun openStorePage(appId: String? = null)

    fun requestReview()

    val updateReady: StateFlow<Boolean>

    fun completeUpdate()

    /** Firebase screen_view; use [logView], which also logs an event of the same name. */
    fun logScreenView(name: String)

    fun logEvent(name: String)

    /** True until the user has answered the in-app question about news notifications. */
    val notificationPromptPending: Boolean

    /** Records the answer; on yes asks the system for permission and subscribes to the news topic. */
    fun answerNotificationPrompt(accepted: Boolean)

    val supportsAdFree: Boolean

    val adFree: StateFlow<AdFreeState>

    fun purchaseAdFree(planId: String)

    fun restorePurchases()

    val subscriptionTermsUrl: String?

    fun manageSubscriptions()

    val supportsRewardedAdFree: Boolean

    val rewardedAd: StateFlow<RewardedAdState>

    fun loadRewardedAd()

    fun showRewardedAd()

    val adPrivacyOptionsRequired: Boolean

    fun showAdPrivacyOptions()

    fun prepareAppStart(onReady: () -> Unit)

    @Composable
    fun AdBanner(modifier: Modifier)
}

/** [rewardUntilMillis] is set while a watched rewarded ad keeps the app ad free (24 h). */
data class AdFreeState(
    val active: Boolean = false,
    val plans: List<AdFreePlan> = emptyList(),
    val rewardUntilMillis: Long? = null,
) {
    val purchasable: Boolean get() = !active && plans.isNotEmpty()
    val adsHidden: Boolean get() = active || rewardUntilMillis != null
}

enum class RewardedAdState { Unavailable, Loading, Ready }

/** [period] and [freeTrial] are ISO-8601 periods such as "P1M" or "P7D"; [freeTrial] is null without an eligible trial. */
data class AdFreePlan(
    val id: String,
    val price: String,
    val period: String,
    val freeTrial: String? = null,
)

val LocalPlatformServices = staticCompositionLocalOf<PlatformServices> {
    error("PlatformServices not provided")
}

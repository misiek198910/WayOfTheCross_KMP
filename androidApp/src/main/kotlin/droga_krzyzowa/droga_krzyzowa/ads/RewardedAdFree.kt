package droga_krzyzowa.droga_krzyzowa.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import droga_krzyzowa.droga_krzyzowa.BuildConfig
import droga_krzyzowa.droga_krzyzowa.platform.RewardedAdState

/** "Watch an ad, get 24 hours without ads". The end of the reward survives restarts. */
class RewardedAdFree private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    private val _until = MutableStateFlow(prefs.getLong(KEY_UNTIL, 0L).takeIf { it > System.currentTimeMillis() })
    val until: StateFlow<Long?> = _until.asStateFlow()

    private val _state = MutableStateFlow(RewardedAdState.Unavailable)
    val state: StateFlow<RewardedAdState> = _state.asStateFlow()

    private var ad: RewardedAd? = null

    init {
        _until.value?.let(::scheduleExpiry)
    }

    /** Ads may only be requested after consent and SDK initialization, see [AdsManager.adsReady]. */
    fun load(context: Context) {
        if (!AdsManager.adsReady || BuildConfig.AD_REWARDED_ID.isEmpty() || ad != null || _state.value == RewardedAdState.Loading) return
        _state.value = RewardedAdState.Loading
        RewardedAd.load(context, BuildConfig.AD_REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(loaded: RewardedAd) {
                ad = loaded
                _state.value = RewardedAdState.Ready
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d(TAG, "Rewarded ad failed to load: ${error.code} ${error.message}")
                _state.value = RewardedAdState.Unavailable
            }
        })
    }

    fun show(activity: Activity) {
        val current = ad ?: return load(activity)
        ad = null
        _state.value = RewardedAdState.Unavailable
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (_until.value == null) load(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "Rewarded ad failed to show: ${error.message}")
                load(activity)
            }
        }
        current.show(activity) { grant() }
    }

    private fun grant() {
        val end = System.currentTimeMillis() + REWARD_MILLIS
        prefs.edit { putLong(KEY_UNTIL, end) }
        _until.value = end
        scheduleExpiry(end)
    }

    private fun scheduleExpiry(end: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            prefs.edit { remove(KEY_UNTIL) }
            _until.value = null
        }, (end - System.currentTimeMillis()).coerceAtLeast(0))
    }

    companion object {
        private const val TAG = "RewardedAdFree"
        private const val PREFS = "rewarded_ad_free"
        private const val KEY_UNTIL = "until_millis"
        private const val REWARD_MILLIS = 24 * 60 * 60 * 1000L

        @Volatile private var instance: RewardedAdFree? = null

        fun get(context: Context): RewardedAdFree =
            instance ?: synchronized(this) { instance ?: RewardedAdFree(context.applicationContext).also { instance = it } }
    }
}

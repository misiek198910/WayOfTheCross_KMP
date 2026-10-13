package droga_krzyzowa.droga_krzyzowa.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import droga_krzyzowa.droga_krzyzowa.BuildConfig

/**
 * Start-up flow: gather consent (UMP) -> initialize Mobile Ads -> show the app open ad.
 * The app continues after the ad is dismissed, fails, or [APP_OPEN_TIMEOUT_MILLIS] elapses.
 */
object AdsManager {
    private const val TAG = "AdsManager"
    private const val APP_OPEN_TIMEOUT_MILLIS = 5_000L

    val bannerUnitId: String = BuildConfig.AD_BANNER_ID
    private val appOpenUnitId: String = BuildConfig.AD_APP_OPEN_ID

    /** Observed by banners: they only load once ads may be requested and the SDK is initialized. */
    var adsReady by mutableStateOf(false)
        private set

    private var started = false

    fun start(activity: Activity, onReady: () -> Unit) {
        val finish = OnceCallback(onReady)
        if (started) {
            finish()
            return
        }
        started = true

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Log.w(TAG, "Consent form: ${it.errorCode} ${it.message}") }
                    if (consentInformation.canRequestAds()) initializeAndShowAppOpen(activity, finish) else finish()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent update: ${requestError.errorCode} ${requestError.message}")
                if (consentInformation.canRequestAds()) initializeAndShowAppOpen(activity, finish) else finish()
            },
        )
    }

    private fun initializeAndShowAppOpen(activity: Activity, finish: OnceCallback) {
        Handler(Looper.getMainLooper()).postDelayed({ finish.timeout() }, APP_OPEN_TIMEOUT_MILLIS)
        MobileAds.initialize(activity) {
            adsReady = true
            AppOpenAd.load(
                activity,
                appOpenUnitId,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        if (finish.done || activity.isFinishing) return
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() = finish()
                            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
                        }
                        finish.holdOff()
                        ad.show(activity)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.d(TAG, "App open ad failed to load: ${error.message}")
                        finish()
                    }
                },
            )
        }
    }

    /** Calls the wrapped callback at most once; a showing ad suspends the timeout via [holdOff]. */
    private class OnceCallback(private val callback: () -> Unit) {
        var done = false
            private set
        private var adShowing = false

        fun holdOff() {
            adShowing = true
        }

        operator fun invoke() {
            if (done) return
            done = true
            callback()
        }

        /** Timeout entry point: ignored while an ad is on screen, its dismissal finishes instead. */
        fun timeout() {
            if (!adShowing) invoke()
        }
    }
}

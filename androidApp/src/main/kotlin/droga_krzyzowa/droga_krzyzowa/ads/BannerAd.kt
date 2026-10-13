package droga_krzyzowa.droga_krzyzowa.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (!AdsManager.adsReady) return
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    // Google's recommended anchored adaptive size; it is recomputed when the width changes (rotation, foldables).
    val adView = remember(widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = AdsManager.bannerUnitId
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
    // AndroidView calls factory only once, so recreate the node whenever the banner size changes.
    key(adView) {
        AndroidView(factory = { adView }, modifier = modifier)
    }
}

package droga_krzyzowa.droga_krzyzowa.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIView

interface IosAds {
    fun start(onReady: (adsAllowed: Boolean) -> Unit)

    val privacyOptionsRequired: Boolean

    fun showPrivacyOptions()

    fun createBanner(widthPoints: Double, landscape: Boolean): UIView

    val rewardedConfigured: Boolean

    /** Calls [onLoaded] with false when ads may not be requested (no consent) or nothing could be loaded. */
    fun loadRewarded(onLoaded: (Boolean) -> Unit)

    /** [onRewarded] fires only when the ad was watched to the end; [onClosed] always fires afterwards. */
    fun showRewarded(onRewarded: () -> Unit, onClosed: () -> Unit)
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun IosBanner(ads: IosAds, modifier: Modifier) {
    val windowSize = LocalWindowInfo.current.containerSize
    val landscape = windowSize.width > windowSize.height
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val width = maxWidth.value.toDouble()
        val banner = remember(width, landscape) { ads.createBanner(width, landscape) }
        val (bannerWidth, bannerHeight) = remember(banner) { banner.frame.useContents { size.width to size.height } }
        key(banner) {
            Box(Modifier.size(bannerWidth.dp, bannerHeight.dp)) {
                UIKitView(factory = { banner }, modifier = Modifier.size(bannerWidth.dp, bannerHeight.dp))
            }
        }
    }
}

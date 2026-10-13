package droga_krzyzowa.droga_krzyzowa

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.russhwolf.settings.NSUserDefaultsSettings
import droga_krzyzowa.droga_krzyzowa.platform.IosAds
import droga_krzyzowa.droga_krzyzowa.platform.IosAnalytics
import droga_krzyzowa.droga_krzyzowa.platform.IosPlatformServices
import droga_krzyzowa.droga_krzyzowa.platform.IosSubscriptions
import platform.Foundation.NSUserDefaults

fun MainViewController(ads: IosAds?, analytics: IosAnalytics?, subscriptions: IosSubscriptions?) = ComposeUIViewController {
    App(
        platform = remember { IosPlatformServices(ads, analytics, subscriptions) },
        settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults),
    )
}

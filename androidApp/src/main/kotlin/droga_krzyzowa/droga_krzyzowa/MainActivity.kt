package droga_krzyzowa.droga_krzyzowa

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.russhwolf.settings.SharedPreferencesSettings
import droga_krzyzowa.droga_krzyzowa.push.PendingStoreRedirect

private const val PREFERENCES_NAME = "app_settings"

class MainActivity : ComponentActivity() {
    private lateinit var platform: AndroidPlatformServices

    /**
     * WebView resets the process default locale to the system language the first time it starts in a
     * process. Ads create WebViews, which would switch the in-app language (see LocalAppLocale) back to
     * the system one mid-session. Starting WebView here, before the app language is applied, gets that
     * reset out of the way; later WebViews leave the locale alone.
     */
    private fun warmUpWebView() {
        runCatching { WebView(applicationContext).destroy() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is always dark, so system bar icons stay light regardless of the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        warmUpWebView()

        val settings = SharedPreferencesSettings(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE))
        LegacyMigration.run(this, settings)
        platform = AndroidPlatformServices(this)

        setContent {
            App(platform = platform, settings = settings)
        }
    }

    override fun onResume() {
        super.onResume()
        // Set by a "promote_parish" push; the old app opened the store the same way.
        PendingStoreRedirect.take(this)?.let(platform::openStorePage)
    }
}

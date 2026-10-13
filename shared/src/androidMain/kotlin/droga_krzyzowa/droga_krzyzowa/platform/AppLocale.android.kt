package droga_krzyzowa.droga_krzyzowa.platform

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        if (default == null) default = Locale.getDefault()
        val locale = value?.let { Locale.forLanguageTag(it) } ?: default!!
        apply(context, configuration, locale)

        // Starting another activity (the app open, interstitial or rewarded ad) makes Android re-apply the
        // system configuration, which resets the process default locale that Compose resources read.
        // Set the app language again whenever this activity comes back, before the next frame is composed.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, locale) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) apply(context, configuration, locale)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        return LocalConfiguration.provides(configuration)
    }

    @Suppress("DEPRECATION")
    private fun apply(context: Context, configuration: Configuration, locale: Locale) {
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        // New WebViews (ads) copy the default locale from the application context's configuration,
        // so it must carry the app language too (see MainActivity.warmUpWebView for the first one).
        val appResources = context.applicationContext.resources
        val appConfiguration = Configuration(appResources.configuration).apply { setLocale(locale) }
        appResources.updateConfiguration(appConfiguration, appResources.displayMetrics)
    }
}

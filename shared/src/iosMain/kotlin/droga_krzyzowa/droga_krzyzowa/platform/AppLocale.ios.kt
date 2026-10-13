package droga_krzyzowa.droga_krzyzowa.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val LANGUAGE_KEY = "AppleLanguages"
    private val default = NSLocale.preferredLanguages.first() as String
    private val locale = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = locale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) {
            defaults.removeObjectForKey(LANGUAGE_KEY)
        } else {
            defaults.setObject(listOf(value), LANGUAGE_KEY)
        }
        return locale.provides(value ?: default)
    }
}

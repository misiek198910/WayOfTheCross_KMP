package droga_krzyzowa.droga_krzyzowa.platform

import droga_krzyzowa.droga_krzyzowa.data.Devotion

/**
 * Names of views and events in Firebase: "DrogaKrzyzowa_…", without Polish letters
 * (Firebase allows letters, digits and "_", at most 40 characters).
 * Constant strings, because class names are not stable across builds.
 */
object Screens {
    private const val PREFIX = "DrogaKrzyzowa_"

    const val HOME = PREFIX + "ekran_glowny"
    const val NEWS = PREFIX + "nowosci"
    const val SETTINGS = PREFIX + "ustawienia"

    fun devotion(devotion: Devotion): String = PREFIX + when (devotion) {
        Devotion.WayOfTheCross -> "droga_krzyzowa"
        Devotion.Lamentations -> "gorzkie_zale"
    }
}

/** Actions in the "Remove ads" card of the settings; the subscription has no screen of its own. */
object Events {
    private const val PREFIX = "DrogaKrzyzowa_"

    const val SUBSCRIPTION_PURCHASE = PREFIX + "subskrypcje_zakup"
    const val SUBSCRIPTION_RESTORE = PREFIX + "subskrypcje_przywroc"
    const val SUBSCRIPTION_MANAGE = PREFIX + "subskrypcje_zarzadzaj"
    const val REWARDED_AD = PREFIX + "reklama_z_nagroda"
}

/** A view is logged as screen_view and also as an event of the same name, so it shows up directly in the event list. */
fun PlatformServices.logView(name: String) {
    logScreenView(name)
    logEvent(name)
}

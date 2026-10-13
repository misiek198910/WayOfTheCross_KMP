package droga_krzyzowa.droga_krzyzowa.platform

/** Firebase Analytics, implemented in Swift (FirebaseAnalyticsBridge.swift). */
interface IosAnalytics {
    fun logScreenView(name: String)

    fun logEvent(name: String)
}

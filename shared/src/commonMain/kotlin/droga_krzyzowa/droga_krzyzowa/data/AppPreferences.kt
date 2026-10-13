package droga_krzyzowa.droga_krzyzowa.data

import com.russhwolf.settings.Settings

enum class AppLanguage(val code: String) {
    Polish("pl"), English("en");
}

class AppPreferences(private val settings: Settings) {

    var language: AppLanguage
        get() = if (settings.getString(KEY_LANGUAGE, AppLanguage.Polish.code) == AppLanguage.English.code) AppLanguage.English else AppLanguage.Polish
        set(value) = settings.putString(KEY_LANGUAGE, value.code)

    var textSize: Int
        get() = settings.getInt(KEY_TEXT_SIZE, 0)
        set(value) = settings.putInt(KEY_TEXT_SIZE, value)

    var newsLastReadId: Int
        get() = settings.getInt(KEY_NEWS_LAST_READ_ID, 0)
        set(value) = settings.putInt(KEY_NEWS_LAST_READ_ID, value)

    /** Page the devotion was left on, so the home screen can offer to continue; 0 means from the start. */
    fun lastPage(devotion: Devotion): Int =
        settings.getInt(KEY_LAST_PAGE + devotion.key, 0).coerceIn(0, devotion.pageCount - 1)

    fun setLastPage(devotion: Devotion, page: Int) = settings.putInt(KEY_LAST_PAGE + devotion.key, page)

    private var rateCount: Int
        get() = settings.getInt(KEY_RATE_COUNT, 0)
        set(value) = settings.putInt(KEY_RATE_COUNT, value)

    fun initLanguageIfNeeded(systemLanguage: String) {
        if (settings.hasKey(KEY_LANGUAGE)) return
        language = if (systemLanguage.lowercase().startsWith("pl")) AppLanguage.Polish else AppLanguage.English
    }

    /** True on the launch that should ask for a rating: the third one, then once every [RATE_CYCLE] launches. */
    fun registerLaunchForRating(): Boolean {
        val count = rateCount
        rateCount = if (count + 1 >= RATE_CYCLE) 0 else count + 1
        return count == RATE_PROMPT_AT
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_NEWS_LAST_READ_ID = "news_last_read_id"
        const val KEY_LAST_PAGE = "last_page_"
        const val KEY_RATE_COUNT = "rate_count"
        const val RATE_PROMPT_AT = 2
        const val RATE_CYCLE = 30
    }
}

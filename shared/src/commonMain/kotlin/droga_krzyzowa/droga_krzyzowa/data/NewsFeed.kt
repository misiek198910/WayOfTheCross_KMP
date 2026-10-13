package droga_krzyzowa.droga_krzyzowa.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NewsItem(
    val id: Int = 0,
    val title: String? = null,
    val content: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("action_link") val actionLink: String? = null,
    @SerialName("publish_date") val publishDate: String? = null,
) {
    val publishedAt: LocalDateTime?
        get() = publishDate?.let { runCatching { LocalDateTime.parse(it.replace(' ', 'T')) }.getOrNull() }
}

class NewsFeed(private val preferences: AppPreferences) {
    sealed interface State {
        data object Loading : State
        data object Error : State
        data class Loaded(val items: List<NewsItem>) : State
    }

    var state by mutableStateOf<State>(State.Loading)
        private set

    var hasUnread by mutableStateOf(false)
        private set

    private val client by lazy { HttpClient { install(HttpTimeout) { requestTimeoutMillis = 20_000 } } }
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun refresh() {
        if (state !is State.Loaded) state = State.Loading
        try {
            val response = client.get(NEWS_URL)
            if (!response.status.isSuccess()) error("HTTP ${response.status}")
            val items = json.decodeFromString<List<NewsItem>>(response.bodyAsText())
            state = State.Loaded(items)
            hasUnread = (items.maxOfOrNull { it.id } ?: 0) > preferences.newsLastReadId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("WayOfTheCross: news feed failed: $e")
            if (state !is State.Loaded) state = State.Error
        }
    }

    fun markAllRead() {
        val newest = (state as? State.Loaded)?.items?.maxOfOrNull { it.id } ?: return
        if (newest > preferences.newsLastReadId) preferences.newsLastReadId = newest
        hasUnread = false
    }

    private companion object {
        const val NEWS_URL = "https://api-parafia.mivs.dev/news"
    }
}

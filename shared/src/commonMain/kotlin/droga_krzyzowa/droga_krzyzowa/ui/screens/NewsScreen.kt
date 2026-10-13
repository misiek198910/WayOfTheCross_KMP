package droga_krzyzowa.droga_krzyzowa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import droga_krzyzowa.droga_krzyzowa.LocalNewsFeed
import droga_krzyzowa.droga_krzyzowa.data.NewsFeed
import droga_krzyzowa.droga_krzyzowa.data.NewsItem
import droga_krzyzowa.droga_krzyzowa.platform.LocalPlatformServices
import droga_krzyzowa.droga_krzyzowa.ui.components.AppScaffold
import droga_krzyzowa.droga_krzyzowa.ui.components.GlassCard
import droga_krzyzowa.droga_krzyzowa.ui.components.GradientButton
import droga_krzyzowa.droga_krzyzowa.ui.components.readableWidth
import droga_krzyzowa.droga_krzyzowa.ui.components.staggeredAppear
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppIcons
import kotlinx.coroutines.launch
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.news_empty
import wayofthecross_kmp.shared.generated.resources.news_error
import wayofthecross_kmp.shared.generated.resources.news_learn_more
import wayofthecross_kmp.shared.generated.resources.news_retry
import wayofthecross_kmp.shared.generated.resources.news_title

@Composable
fun NewsScreen(onBack: () -> Unit) {
    val newsFeed = LocalNewsFeed.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { newsFeed.refresh() }
    val state = newsFeed.state
    LaunchedEffect(state) { if (state is NewsFeed.State.Loaded) newsFeed.markAllRead() }

    AppScaffold(title = stringResource(Res.string.news_title), onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                NewsFeed.State.Loading -> CircularProgressIndicator(color = AppColors.Gold, modifier = Modifier.align(Alignment.Center))
                NewsFeed.State.Error -> Column(
                    Modifier.align(Alignment.Center).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        stringResource(Res.string.news_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    GradientButton(
                        text = stringResource(Res.string.news_retry),
                        onClick = { scope.launch { newsFeed.refresh() } },
                        icon = AppIcons.Restart,
                    )
                }
                is NewsFeed.State.Loaded -> if (state.items.isEmpty()) {
                    Text(
                        stringResource(Res.string.news_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                            NewsCard(item, Modifier.readableWidth().staggeredAppear(minOf(index, 4)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(item: NewsItem, modifier: Modifier = Modifier) {
    val platform = LocalPlatformServices.current
    GlassCard(contentPadding = PaddingValues(0.dp), modifier = modifier.fillMaxWidth()) {
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.publishedAt?.let { date ->
                Text(
                    "${date.day.toString().padStart(2, '0')}.${date.month.number.toString().padStart(2, '0')}.${date.year}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Gold,
                )
            }
            if (!item.title.isNullOrBlank()) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            if (!item.content.isNullOrBlank()) {
                Text(linkified(item.content), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            }
            if (!item.actionLink.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                GradientButton(
                    text = stringResource(Res.string.news_learn_more),
                    onClick = { platform.openUrl(item.actionLink) },
                    icon = AppIcons.OpenInNew,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val urlPattern = Regex("""https?://\S+""")

private fun linkified(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in urlPattern.findAll(text)) {
        append(text, last, match.range.first)
        withLink(LinkAnnotation.Url(match.value, TextLinkStyles(SpanStyle(color = AppColors.Gold, textDecoration = TextDecoration.Underline)))) {
            append(match.value)
        }
        last = match.range.last + 1
    }
    append(text, last, text.length)
}

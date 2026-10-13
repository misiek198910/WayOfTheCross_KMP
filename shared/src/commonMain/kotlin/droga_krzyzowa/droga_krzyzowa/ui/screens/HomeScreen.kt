package droga_krzyzowa.droga_krzyzowa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import droga_krzyzowa.droga_krzyzowa.LocalNewsFeed
import droga_krzyzowa.droga_krzyzowa.LocalPreferences
import droga_krzyzowa.droga_krzyzowa.data.Devotion
import droga_krzyzowa.droga_krzyzowa.platform.LocalPlatformServices
import droga_krzyzowa.droga_krzyzowa.ui.components.GlassCard
import droga_krzyzowa.droga_krzyzowa.ui.components.MaxContentWidth
import droga_krzyzowa.droga_krzyzowa.ui.components.TwoColumnMinWidth
import droga_krzyzowa.droga_krzyzowa.ui.components.UnreadDot
import droga_krzyzowa.droga_krzyzowa.ui.components.readableWidth
import droga_krzyzowa.droga_krzyzowa.ui.components.staggeredAppear
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.app_name
import wayofthecross_kmp.shared.generated.resources.emblem
import wayofthecross_kmp.shared.generated.resources.home_continue
import wayofthecross_kmp.shared.generated.resources.home_continue_station
import wayofthecross_kmp.shared.generated.resources.home_lamentations_desc
import wayofthecross_kmp.shared.generated.resources.home_settings
import wayofthecross_kmp.shared.generated.resources.home_way_of_cross_desc

private var ratePromptChecked = false

@Composable
fun HomeScreen(
    onDevotion: (Devotion) -> Unit,
    onSettings: () -> Unit,
) {
    val preferences = LocalPreferences.current
    val platform = LocalPlatformServices.current

    LaunchedEffect(Unit) {
        if (!ratePromptChecked) {
            ratePromptChecked = true
            if (preferences.registerLaunchForRating()) platform.requestReview()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { platform.AdBanner(Modifier.fillMaxWidth().navigationBarsPadding()) },
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            val twoColumns = maxWidth >= TwoColumnMinWidth
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .readableWidth(if (twoColumns) MaxContentWidth * 1.5f else MaxContentWidth)
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp).staggeredAppear(0),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(Res.drawable.emblem), contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.Gold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onSettings) {
                        // News live in the settings now; the dot tells there is something unread there.
                        Box {
                            Icon(AppIcons.Settings, contentDescription = stringResource(Res.string.home_settings), tint = AppColors.GoldLight)
                            if (LocalNewsFeed.current.hasUnread) UnreadDot(Modifier.align(Alignment.TopEnd))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                val cards: List<@Composable (Modifier) -> Unit> = listOf(
                    { m ->
                        DevotionCard(
                            devotion = Devotion.WayOfTheCross,
                            label = Res.string.home_way_of_cross_desc,
                            highlighted = true,
                            onClick = { onDevotion(Devotion.WayOfTheCross) },
                            modifier = m,
                        )
                    },
                    { m ->
                        DevotionCard(
                            devotion = Devotion.Lamentations,
                            label = Res.string.home_lamentations_desc,
                            highlighted = false,
                            onClick = { onDevotion(Devotion.Lamentations) },
                            modifier = m,
                        )
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.chunked(if (twoColumns) 2 else 1).forEachIndexed { row, rowCards ->
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowCards.forEachIndexed { column, card ->
                                card(Modifier.weight(1f).fillMaxHeight().staggeredAppear(1 + row * 2 + column))
                            }
                            // Keeps a lone last card at half width in the two-column grid.
                            if (twoColumns && rowCards.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DevotionCard(
    devotion: Devotion,
    label: StringResource,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Read on every visit: the home destination is recomposed when the user comes back from a devotion.
    val lastPage = LocalPreferences.current.lastPage(devotion)
    GlassCard(onClick = onClick, highlighted = highlighted, contentPadding = PaddingValues(0.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxHeight().heightIn(min = 140.dp), verticalAlignment = Alignment.CenterVertically) {
            // matchParentSize keeps the large picture out of the row's intrinsic height, the text decides it.
            Box(Modifier.width(108.dp).fillMaxHeight()) {
                Image(
                    painterResource(devotion.image(0, landscape = false)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Column(Modifier.weight(1f).padding(20.dp)) {
                Text(
                    stringResource(label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Gold,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(devotion.titleRes), style = MaterialTheme.typography.titleLarge, color = Color.White)
                if (lastPage > 0) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (lastPage + 1) / devotion.pageCount.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AppColors.Gold,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${stringResource(Res.string.home_continue)} · " +
                            stringResource(Res.string.home_continue_station, lastPage + 1, devotion.pageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            }
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.GoldLight, modifier = Modifier.padding(end = 16.dp))
        }
    }
}

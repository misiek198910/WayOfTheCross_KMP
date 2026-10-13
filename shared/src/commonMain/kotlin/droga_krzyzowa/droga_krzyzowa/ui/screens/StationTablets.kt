package droga_krzyzowa.droga_krzyzowa.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import droga_krzyzowa.droga_krzyzowa.data.Devotion
import droga_krzyzowa.droga_krzyzowa.data.romanNumeral
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import droga_krzyzowa.droga_krzyzowa.ui.theme.LocalLora
import org.jetbrains.compose.resources.stringResource

/** A stone tablet with an arched top, like the small plaques that mark the stations in a church. */
private val TabletShape = GenericShape { size, _ ->
    val archHeight = size.width * 0.38f
    moveTo(0f, archHeight)
    arcTo(Rect(0f, 0f, size.width, archHeight * 2), 180f, 180f, false)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

private val Limestone = listOf(Color(0xFFE8E1D2), Color(0xFFB3A994))
private val LimestoneEdge = Color(0xFF8A806C)
private val Engraving = Color(0xFF3B3326)
private val GoldStone = listOf(AppColors.GoldBright, AppColors.Gold)

private val TabletWidth = 34.dp
private val TabletHeight = 44.dp
private val TabletSpacing = 8.dp

/**
 * Progress through the devotion as a row of numbered stone tablets: the current one is gold and raised,
 * the ones already prayed are light stone, the ones ahead are dimmed. Tapping a tablet jumps to it.
 * With more tablets than fit, the row scrolls and keeps the current one centred.
 */
@Composable
fun StationTablets(devotion: Devotion, current: Int, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(current) {
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull()?.size ?: return@LaunchedEffect listState.scrollToItem(current)
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        listState.animateScrollToItem(current, scrollOffset = -(viewport - item) / 2)
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(TabletSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(TabletHeight + 12.dp)
            .fadingEdges(),
    ) {
        items(devotion.pageCount) { index ->
            val label = stringResource(devotion.pageLabelRes, romanNumeral(index + 1))
            Tablet(
                numeral = romanNumeral(index + 1),
                state = when {
                    index == current -> TabletState.Current
                    index < current -> TabletState.Done
                    else -> TabletState.Ahead
                },
                modifier = Modifier
                    .semantics {
                        contentDescription = label
                        selected = index == current
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                    ) { onClick(index) },
            )
        }
    }
}

private enum class TabletState { Current, Done, Ahead }

@Composable
private fun Tablet(numeral: String, state: TabletState, modifier: Modifier = Modifier) {
    val current = state == TabletState.Current
    val scale by animateFloatAsState(if (current) 1.15f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "tabletScale")
    val alpha by animateFloatAsState(if (state == TabletState.Ahead) 0.55f else 1f, label = "tabletAlpha")
    val edge by animateColorAsState(if (current) AppColors.GoldLight else LimestoneEdge, label = "tabletEdge")
    Box(
        modifier
            .size(TabletWidth, TabletHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                // Lifts the current tablet a little, like a plaque catching the light.
                shadowElevation = if (current) 6.dp.toPx() else 0f
                shape = TabletShape
                clip = true
            }
            .background(Brush.verticalGradient(if (current) GoldStone else Limestone))
            .border(1.dp, edge, TabletShape)
            .clip(TabletShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            numeral,
            color = if (current) AppColors.DeepPurple else Engraving,
            fontFamily = LocalLora.current,
            fontWeight = FontWeight.Bold,
            fontSize = if (numeral.length > 3) 10.sp else 12.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Fades the row out at both ends, so tablets scrolling away do not end with a hard cut. */
private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Transparent,
                0.05f to Color.Black,
                0.95f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

package droga_krzyzowa.droga_krzyzowa.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import droga_krzyzowa.droga_krzyzowa.platform.LocalPlatformServices
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppIcons
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.ad_free_banner_link
import wayofthecross_kmp.shared.generated.resources.back

@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "background")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 24_000, easing = LinearEasing)),
        label = "phase",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppColors.DeepPurple, AppColors.DeepPurpleLight, AppColors.Plum)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun glow(color: Color, center: Offset, radius: Float) = drawCircle(
                brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
                radius = radius,
                center = center,
            )
            val r = size.minDimension
            glow(
                AppColors.Violet.copy(alpha = 0.30f),
                Offset(size.width * (0.2f + 0.15f * cos(phase)), size.height * (0.25f + 0.08f * sin(phase))),
                r * 0.9f,
            )
            glow(
                AppColors.Gold.copy(alpha = 0.13f),
                Offset(size.width * (0.85f - 0.12f * sin(phase)), size.height * (0.75f + 0.06f * cos(phase))),
                r * 0.8f,
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val platform = LocalPlatformServices.current
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(AppIcons.ArrowBack, contentDescription = stringResource(Res.string.back))
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = AppColors.GoldLight,
                    navigationIconContentColor = AppColors.Gold,
                    actionIconContentColor = AppColors.Gold,
                ),
            )
        },
        bottomBar = { AppAdBanner(Modifier.fillMaxWidth().navigationBarsPadding()) },
        content = content,
    )
}

/** Opens the "Remove ads" card in the settings; null on the settings screen itself. */
val LocalOpenAdFreeSettings = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The bottom banner with a small "Turn off ads" link above it, when there is a way to turn ads off. */
@Composable
fun AppAdBanner(modifier: Modifier = Modifier) {
    val platform = LocalPlatformServices.current
    val openAdFree = LocalOpenAdFreeSettings.current
    val canRemoveAds = platform.supportsAdFree || platform.supportsRewardedAdFree
    platform.AdBanner(modifier) {
        if (openAdFree != null && canRemoveAds) RemoveAdsLink(openAdFree)
    }
}

@Composable
private fun RemoveAdsLink(onClick: () -> Unit) {
    // Zero layout height: the pill floats over the content above the banner instead of taking its space.
    Box(
        Modifier.fillMaxWidth().height(0.dp).wrapContentHeight(Alignment.Bottom, unbounded = true)
            .padding(end = 8.dp, bottom = 6.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(AppColors.DeepPurple.copy(alpha = 0.85f))
                .border(1.dp, AppColors.Gold.copy(alpha = 0.35f), RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Close, contentDescription = null, tint = AppColors.Gold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.ad_free_banner_link), style = MaterialTheme.typography.labelMedium, color = AppColors.GoldLight)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 600f), label = "press")
    val borderColor = if (highlighted) AppColors.Gold.copy(alpha = 0.85f) else AppColors.GlassBorder
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))
                )
            )
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 600f), label = "press")
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .heightIn(min = 52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(AppColors.Gold, AppColors.GoldBright)))
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = AppColors.DeepPurple, modifier = Modifier.size(20.dp))
            Box(Modifier.size(10.dp))
        }
        Text(text, color = AppColors.DeepPurple, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = AppColors.GoldLight) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(AppColors.Gold.copy(alpha = 0.45f), AppColors.Violet.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.Gold,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 8.dp, start = 4.dp),
    )
}

val MaxContentWidth = 640.dp

val TwoColumnMinWidth = 600.dp

/** Put it after the scroll modifier, so on wide windows the whole window still scrolls, not just the centred column. */
fun Modifier.readableWidth(max: Dp = MaxContentWidth): Modifier =
    wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)

@Composable
fun UnreadDot(modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(AppColors.Gold))
}

@Composable
fun Modifier.staggeredAppear(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        progress.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 40.dp.toPx()
    }
}

package droga_krzyzowa.droga_krzyzowa.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import droga_krzyzowa.droga_krzyzowa.LocalPreferences
import droga_krzyzowa.droga_krzyzowa.data.AppLanguage
import droga_krzyzowa.droga_krzyzowa.data.Devotion
import droga_krzyzowa.droga_krzyzowa.data.loadDevotionText
import droga_krzyzowa.droga_krzyzowa.data.romanNumeral
import droga_krzyzowa.droga_krzyzowa.platform.LocalPlatformServices
import droga_krzyzowa.droga_krzyzowa.ui.components.AppAdBanner
import droga_krzyzowa.droga_krzyzowa.ui.components.GlassCard
import droga_krzyzowa.droga_krzyzowa.ui.components.GradientButton
import droga_krzyzowa.droga_krzyzowa.ui.components.TwoColumnMinWidth
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppIcons
import droga_krzyzowa.droga_krzyzowa.ui.theme.LocalLora
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.back
import wayofthecross_kmp.shared.generated.resources.devotion_next
import wayofthecross_kmp.shared.generated.resources.devotion_previous
import wayofthecross_kmp.shared.generated.resources.devotion_read
import wayofthecross_kmp.shared.generated.resources.text_size

private const val MIN_TEXT_SIZE = 14f
private const val MAX_TEXT_SIZE = 30f
private const val DEFAULT_TEXT_SIZE = 18f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionScreen(devotion: Devotion, language: AppLanguage, onBack: () -> Unit) {
    val preferences = LocalPreferences.current
    val platform = LocalPlatformServices.current
    val lastPage = devotion.pageCount - 1
    val pagerState = rememberPagerState(initialPage = remember { preferences.lastPage(devotion) }) { devotion.pageCount }
    val scope = rememberCoroutineScope()
    val titles = stringArrayResource(devotion.pageTitlesRes)
    var showText by rememberSaveable { mutableStateOf(false) }
    var textSize by remember {
        mutableFloatStateOf(preferences.textSize.takeIf { it > 0 }?.toFloat()?.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE) ?: DEFAULT_TEXT_SIZE)
    }
    val reading = ReadingState(
        textSize = textSize,
        onTextSizeChange = { textSize = it },
        onTextSizeChangeFinished = { preferences.textSize = textSize.roundToInt() },
    )

    // Reaching the last station or part finishes the devotion, so the next visit starts from the beginning.
    LaunchedEffect(pagerState.currentPage) {
        preferences.setLastPage(devotion, if (pagerState.currentPage == lastPage) 0 else pagerState.currentPage)
    }

    fun goTo(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page.coerceIn(0, lastPage)) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Like the previous app, no banner on phones in landscape: the picture and the text need the height.
        val compactHeight = maxHeight < 480.dp
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = { if (!compactHeight) AppAdBanner(Modifier.fillMaxWidth().navigationBarsPadding()) },
        ) { innerPadding ->
            val bottom = innerPadding.calculateBottomPadding()
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = bottom)
                    .consumeWindowInsets(PaddingValues(bottom = bottom))
            ) {
                val sideBySide = maxWidth > maxHeight || (maxWidth >= TwoColumnMinWidth && maxWidth >= maxHeight * 0.8f)
                if (sideBySide) {
                    SideBySideLayout(devotion, language, pagerState, titles, reading, onBack, ::goTo)
                } else {
                    FullBleedLayout(devotion, pagerState, titles, onBack, ::goTo, onRead = { showText = true })
                }
            }
        }
    }

    if (showText) {
        val page = pagerState.currentPage
        ModalBottomSheet(
            onDismissRequest = { showText = false },
            sheetState = rememberBottomSheetState(SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = AppColors.GoldLight,
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                ReadingHeader(devotion, page, titles, reading)
                Spacer(Modifier.height(16.dp))
                DevotionText(devotion, page, language, reading.textSize)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private class ReadingState(
    val textSize: Float,
    val onTextSizeChange: (Float) -> Unit,
    val onTextSizeChangeFinished: () -> Unit,
)

/** Phones in portrait: the picture fills the screen, the meditation opens in a bottom sheet. */
@Composable
private fun FullBleedLayout(
    devotion: Devotion,
    pagerState: PagerState,
    titles: List<String>,
    onBack: () -> Unit,
    goTo: (Int) -> Unit,
    onRead: () -> Unit,
) {
    val page = pagerState.currentPage
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { index ->
            Image(
                painterResource(devotion.image(index, landscape = false)),
                contentDescription = titles.getOrNull(index),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRead),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.22f to Color.Transparent,
                        0.5f to Color.Transparent,
                        0.82f to AppColors.DeepPurple.copy(alpha = 0.92f),
                        1f to AppColors.DeepPurple,
                    )
                )
        )

        // Only the back button on top: the stone tablets below already show where the user is.
        RoundIconButton(
            AppIcons.ArrowBack,
            stringResource(Res.string.back),
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            PageTitle(page, titles)
            Spacer(Modifier.height(12.dp))
            StationTablets(devotion, page, goTo)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(AppIcons.ChevronLeft, stringResource(Res.string.devotion_previous), enabled = page > 0) { goTo(page - 1) }
                GradientButton(
                    text = stringResource(Res.string.devotion_read),
                    onClick = onRead,
                    icon = AppIcons.Book,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                StepButton(AppIcons.ChevronRight, stringResource(Res.string.devotion_next), enabled = page < devotion.pageCount - 1) {
                    goTo(page + 1)
                }
            }
        }
    }
}

/** Landscape, tablets and unfolded foldables: picture on the left, the text always visible on the right. */
@Composable
private fun SideBySideLayout(
    devotion: Devotion,
    language: AppLanguage,
    pagerState: PagerState,
    titles: List<String>,
    reading: ReadingState,
    onBack: () -> Unit,
    goTo: (Int) -> Unit,
) {
    val page = pagerState.currentPage
    Row(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(16.dp),
    ) {
        BoxWithConstraints(Modifier.weight(1.1f).fillMaxHeight().clip(RoundedCornerShape(28.dp))) {
            val landscape = maxWidth > maxHeight
            HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { index ->
                Image(
                    painterResource(devotion.image(index, landscape)),
                    contentDescription = titles.getOrNull(index),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to AppColors.DeepPurple.copy(alpha = 0.9f)))
            )
            Box(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                PageTitle(page, titles)
            }
            RoundIconButton(AppIcons.ArrowBack, stringResource(Res.string.back), onClick = onBack, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            GlassCard(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    ReadingHeader(devotion, page, titles, reading)
                    Spacer(Modifier.height(12.dp))
                    DevotionText(devotion, page, language, reading.textSize)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(AppIcons.ChevronLeft, stringResource(Res.string.devotion_previous), enabled = page > 0) { goTo(page - 1) }
                StationTablets(devotion, page, goTo, Modifier.weight(1f))
                StepButton(AppIcons.ChevronRight, stringResource(Res.string.devotion_next), enabled = page < devotion.pageCount - 1) {
                    goTo(page + 1)
                }
            }
        }
    }
}

@Composable
private fun PageTitle(page: Int, titles: List<String>) {
    AnimatedContent(
        targetState = page,
        transitionSpec = { (fadeIn(tween(400)) + slideInVertically { it / 3 }) togetherWith fadeOut(tween(200)) },
        label = "pageTitle",
    ) { shown ->
        // No "Station IV" label: the stone tablets below already show the number.
        Text(titles.getOrElse(shown) { "" }, style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}

@Composable
private fun ReadingHeader(devotion: Devotion, page: Int, titles: List<String>, reading: ReadingState) {
    var showSizeControl by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(devotion.pageLabelRes, romanNumeral(page + 1)).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Gold,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(titles.getOrElse(page) { "" }, style = MaterialTheme.typography.headlineSmall, color = AppColors.Gold)
        }
        IconButton(onClick = { showSizeControl = !showSizeControl }) {
            Icon(AppIcons.TextSize, contentDescription = stringResource(Res.string.text_size), tint = AppColors.GoldLight)
        }
    }
    AnimatedVisibility(visible = showSizeControl, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("A", fontSize = 14.sp, color = AppColors.TextSecondary)
            Spacer(Modifier.width(12.dp))
            Slider(
                value = reading.textSize,
                onValueChange = reading.onTextSizeChange,
                onValueChangeFinished = reading.onTextSizeChangeFinished,
                valueRange = MIN_TEXT_SIZE..MAX_TEXT_SIZE,
                colors = SliderDefaults.colors(thumbColor = AppColors.Gold, activeTrackColor = AppColors.Gold),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text("A", fontSize = 24.sp, color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun DevotionText(devotion: Devotion, page: Int, language: AppLanguage, textSize: Float) {
    val text by produceState<AnnotatedString?>(null, devotion, page, language) {
        value = runCatching { loadDevotionText(devotion, page, language, AppColors.Gold) }.getOrDefault(AnnotatedString(""))
    }
    AnimatedVisibility(visible = text != null, enter = fadeIn(tween(300))) {
        SelectionContainer {
            // Lora with generous leading, like the reader in SlaveOfMary.
            Text(
                text ?: AnnotatedString(""),
                style = TextStyle(
                    fontFamily = LocalLora.current,
                    fontSize = textSize.sp,
                    lineHeight = 1.6.em,
                    color = AppColors.GoldLight,
                ),
            )
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = AppColors.DeepPurple.copy(alpha = 0.55f),
            contentColor = AppColors.Gold,
        ),
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.White.copy(alpha = 0.12f),
            contentColor = AppColors.Gold,
            disabledContainerColor = Color.White.copy(alpha = 0.04f),
            disabledContentColor = AppColors.Gold.copy(alpha = 0.3f),
        ),
    ) {
        Icon(icon, contentDescription = description)
    }
}

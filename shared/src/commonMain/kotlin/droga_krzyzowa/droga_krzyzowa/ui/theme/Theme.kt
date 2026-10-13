package droga_krzyzowa.droga_krzyzowa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.lora
import wayofthecross_kmp.shared.generated.resources.lora_italic

/** The purple and gold palette of the original app (GoldPrimary, GoldLight, DeepPurple, DeepPurpleLight). */
object AppColors {
    val DeepPurple = Color(0xFF1A0B2E)
    val DeepPurpleLight = Color(0xFF2D164F)
    val Plum = Color(0xFF3F1F66)
    val Violet = Color(0xFF7B4FD0)
    val Gold = Color(0xFFD4AF37)
    val GoldLight = Color(0xFFF9E4B7)
    val GoldBright = Color(0xFFEBCB6B)
    val Glass = Color.White.copy(alpha = 0.08f)
    val GlassBorder = Color(0xFFD4AF37).copy(alpha = 0.28f)
    val TextSecondary = Color(0xFFF9E4B7).copy(alpha = 0.72f)
}

private val colorScheme = darkColorScheme(
    primary = AppColors.Gold,
    onPrimary = AppColors.DeepPurple,
    primaryContainer = Color(0xFF574416),
    onPrimaryContainer = AppColors.GoldLight,
    secondary = AppColors.GoldLight,
    onSecondary = AppColors.DeepPurple,
    secondaryContainer = AppColors.DeepPurpleLight,
    onSecondaryContainer = AppColors.GoldLight,
    tertiary = Color(0xFFC7A8FF),
    onTertiary = AppColors.DeepPurple,
    background = AppColors.DeepPurple,
    onBackground = Color.White,
    surface = Color(0xFF1F0F36),
    onSurface = Color.White,
    surfaceVariant = AppColors.DeepPurpleLight,
    onSurfaceVariant = AppColors.TextSecondary,
    surfaceContainerLowest = AppColors.DeepPurple,
    surfaceContainerLow = Color(0xFF22103B),
    surfaceContainer = Color(0xFF281443),
    surfaceContainerHigh = AppColors.DeepPurpleLight,
    surfaceContainerHighest = Color(0xFF371C5E),
    outline = AppColors.Gold.copy(alpha = 0.5f),
    outlineVariant = Color.White.copy(alpha = 0.12f),
)

/** Serif Lora (variable font, as in SlaveOfMary) available everywhere; created once in [WayOfTheCrossTheme]. */
val LocalLora = staticCompositionLocalOf<FontFamily> { FontFamily.Serif }

@Composable
private fun rememberLoraFamily(): FontFamily {
    @Composable
    fun weighted(w: Int, italic: Boolean = false) = Font(
        resource = if (italic) Res.font.lora_italic else Res.font.lora,
        weight = FontWeight(w),
        style = if (italic) FontStyle.Italic else FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(w)),
    )
    val fonts = listOf(
        weighted(400), weighted(500), weighted(600), weighted(700),
        weighted(400, italic = true), weighted(700, italic = true),
    )
    return remember(fonts) { FontFamily(fonts) }
}

/** Lora headlines give the devotional texts a calmer, more classic feel. */
private fun appTypography(serif: FontFamily): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = serif),
        displayMedium = base.displayMedium.copy(fontFamily = serif),
        displaySmall = base.displaySmall.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun WayOfTheCrossTheme(content: @Composable () -> Unit) {
    val lora = rememberLoraFamily()
    val typography = remember(lora) { appTypography(lora) }
    CompositionLocalProvider(LocalLora provides lora) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}

package droga_krzyzowa.droga_krzyzowa.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import droga_krzyzowa.droga_krzyzowa.LocalNewsFeed
import droga_krzyzowa.droga_krzyzowa.data.AppLanguage
import droga_krzyzowa.droga_krzyzowa.platform.AdFreePlan
import droga_krzyzowa.droga_krzyzowa.platform.Events
import droga_krzyzowa.droga_krzyzowa.platform.LocalPlatformServices
import droga_krzyzowa.droga_krzyzowa.platform.RewardedAdState
import droga_krzyzowa.droga_krzyzowa.ui.components.AppScaffold
import droga_krzyzowa.droga_krzyzowa.ui.components.GlassCard
import droga_krzyzowa.droga_krzyzowa.ui.components.GradientButton
import droga_krzyzowa.droga_krzyzowa.ui.components.IconBadge
import droga_krzyzowa.droga_krzyzowa.ui.components.SectionTitle
import droga_krzyzowa.droga_krzyzowa.ui.components.UnreadDot
import droga_krzyzowa.droga_krzyzowa.ui.components.readableWidth
import droga_krzyzowa.droga_krzyzowa.ui.components.staggeredAppear
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppIcons
import kotlin.math.sqrt
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.ad_free_active_text
import wayofthecross_kmp.shared.generated.resources.ad_free_active_title
import wayofthecross_kmp.shared.generated.resources.ad_free_days
import wayofthecross_kmp.shared.generated.resources.ad_free_manage
import wayofthecross_kmp.shared.generated.resources.ad_free_months
import wayofthecross_kmp.shared.generated.resources.ad_free_plan_button
import wayofthecross_kmp.shared.generated.resources.ad_free_plan_monthly
import wayofthecross_kmp.shared.generated.resources.ad_free_plan_yearly
import wayofthecross_kmp.shared.generated.resources.ad_free_plan_yearly_trial
import wayofthecross_kmp.shared.generated.resources.ad_free_privacy
import wayofthecross_kmp.shared.generated.resources.ad_free_restore
import wayofthecross_kmp.shared.generated.resources.ad_free_reward_active
import wayofthecross_kmp.shared.generated.resources.ad_free_reward_loading
import wayofthecross_kmp.shared.generated.resources.ad_free_reward_text
import wayofthecross_kmp.shared.generated.resources.ad_free_reward_unavailable
import wayofthecross_kmp.shared.generated.resources.ad_free_reward_watch
import wayofthecross_kmp.shared.generated.resources.ad_free_subscribe
import wayofthecross_kmp.shared.generated.resources.ad_free_terms
import wayofthecross_kmp.shared.generated.resources.ad_free_text
import wayofthecross_kmp.shared.generated.resources.ad_free_title
import wayofthecross_kmp.shared.generated.resources.ad_free_unavailable
import wayofthecross_kmp.shared.generated.resources.home_news
import wayofthecross_kmp.shared.generated.resources.home_settings
import wayofthecross_kmp.shared.generated.resources.lang_english
import wayofthecross_kmp.shared.generated.resources.lang_polish
import wayofthecross_kmp.shared.generated.resources.settings_ad_privacy
import wayofthecross_kmp.shared.generated.resources.settings_ads
import wayofthecross_kmp.shared.generated.resources.settings_language
import wayofthecross_kmp.shared.generated.resources.settings_more
import wayofthecross_kmp.shared.generated.resources.settings_rate
import wayofthecross_kmp.shared.generated.resources.settings_version

private const val PRIVACY_POLICY_URL = "https://misiek198910.github.io/mojaparafia-privacy/droga_krzyzowa_privacy.html"

@Composable
fun SettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onNews: () -> Unit,
) {
    val platform = LocalPlatformServices.current
    AppScaffold(title = stringResource(Res.string.home_settings), onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .readableWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(Res.string.settings_language))
            val options = listOf(AppLanguage.Polish to Res.string.lang_polish, AppLanguage.English to Res.string.lang_english)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().staggeredAppear(0)) {
                options.forEachIndexed { index, (option, label) ->
                    SegmentedButton(
                        selected = language == option,
                        onClick = { onLanguageChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = AppColors.Gold.copy(alpha = 0.25f),
                            activeContentColor = AppColors.GoldLight,
                            activeBorderColor = AppColors.Gold.copy(alpha = 0.6f),
                            inactiveContainerColor = Color.White.copy(alpha = 0.06f),
                            inactiveContentColor = AppColors.TextSecondary,
                            inactiveBorderColor = AppColors.GlassBorder,
                        ),
                    ) {
                        Text(stringResource(label))
                    }
                }
            }

            val adFreeState by platform.adFree.collectAsState()
            val showAdPrivacy = !adFreeState.active && platform.adPrivacyOptionsRequired
            val showAdFreeCard = platform.supportsAdFree || platform.supportsRewardedAdFree
            if (showAdFreeCard || showAdPrivacy) {
                Spacer(Modifier.height(8.dp))
                SectionTitle(stringResource(Res.string.settings_ads))
                if (showAdFreeCard) AdFreeCard(Modifier.staggeredAppear(1))
                if (showAdPrivacy) {
                    SettingsRow(AppIcons.PrivacyTip, stringResource(Res.string.settings_ad_privacy), platform::showAdPrivacyOptions, Modifier.staggeredAppear(1))
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(Res.string.settings_more))
            var row = 2
            SettingsRow(
                AppIcons.Campaign,
                stringResource(Res.string.home_news),
                onNews,
                Modifier.staggeredAppear(row++),
                unread = LocalNewsFeed.current.hasUnread,
            )
            if (platform.supportsStoreLinks) {
                SettingsRow(AppIcons.Star, stringResource(Res.string.settings_rate), { platform.openStorePage() }, Modifier.staggeredAppear(row++))
            }
            SettingsRow(AppIcons.PrivacyTip, stringResource(Res.string.ad_free_privacy), { platform.openUrl(PRIVACY_POLICY_URL) }, Modifier.staggeredAppear(row++))

            if (platform.versionName.isNotEmpty()) {
                Text(
                    stringResource(Res.string.settings_version, platform.versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier, unread: Boolean = false) {
    GlassCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
            if (unread) {
                UnreadDot()
                Spacer(Modifier.width(8.dp))
            }
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun AdFreeCard(modifier: Modifier = Modifier) {
    val platform = LocalPlatformServices.current
    val state by platform.adFree.collectAsState()
    GlassCard(highlighted = state.active, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (state.active) AppIcons.Check else AppIcons.Block, tint = if (state.active) AppColors.Gold else AppColors.GoldLight)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (state.active) Res.string.ad_free_active_title else Res.string.ad_free_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    when {
                        state.active -> stringResource(Res.string.ad_free_active_text)
                        !platform.supportsAdFree -> stringResource(Res.string.ad_free_reward_text)
                        state.plans.isEmpty() -> stringResource(Res.string.ad_free_unavailable)
                        else -> stringResource(Res.string.ad_free_text)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.active) AppColors.Gold else AppColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (state.active) {
            OutlinedButton(onClick = { platform.logEvent(Events.SUBSCRIPTION_MANAGE); platform.manageSubscriptions() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(Res.string.ad_free_manage), color = AppColors.GoldLight)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (platform.supportsAdFree && state.plans.isEmpty()) {
                    GradientButton(text = stringResource(Res.string.ad_free_subscribe), onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
                }
                state.plans.forEach { plan ->
                    GradientButton(
                        text = stringResource(Res.string.ad_free_plan_button, planLabel(plan), plan.price),
                        onClick = {
                            platform.logEvent(Events.SUBSCRIPTION_PURCHASE)
                            platform.purchaseAdFree(plan.id)
                        },
                        enabled = state.purchasable,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (platform.supportsRewardedAdFree) RewardedAdFreeRow(state.rewardUntilMillis)
            }
            if (platform.supportsAdFree) {
                TextButton(onClick = { platform.logEvent(Events.SUBSCRIPTION_RESTORE); platform.restorePurchases() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(Res.string.ad_free_restore), color = AppColors.TextSecondary)
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            platform.subscriptionTermsUrl?.takeIf { platform.supportsAdFree }?.let { terms ->
                LegalLink(stringResource(Res.string.ad_free_terms)) { platform.openUrl(terms) }
                Text("·", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            LegalLink(stringResource(Res.string.ad_free_privacy)) { platform.openUrl(PRIVACY_POLICY_URL) }
        }
    }
}

@Composable
private fun RewardedAdFreeRow(rewardUntilMillis: Long?) {
    val platform = LocalPlatformServices.current
    val adState by platform.rewardedAd.collectAsState()
    LaunchedEffect(rewardUntilMillis) { if (rewardUntilMillis == null) platform.loadRewardedAd() }
    var retried by remember { mutableStateOf(false) }
    LaunchedEffect(adState) {
        if (retried && adState == RewardedAdState.Ready) {
            retried = false
            platform.logEvent(Events.REWARDED_AD)
            platform.showRewardedAd()
        }
    }
    when {
        rewardUntilMillis != null -> {
            val end = Instant.fromEpochMilliseconds(rewardUntilMillis).toLocalDateTime(TimeZone.currentSystemDefault())
            val time = "${end.day.toString().padStart(2, '0')}.${end.month.number.toString().padStart(2, '0')}, " +
                "${end.hour.toString().padStart(2, '0')}:${end.minute.toString().padStart(2, '0')}"
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Check, contentDescription = null, tint = AppColors.Gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.ad_free_reward_active, time), style = MaterialTheme.typography.bodyMedium, color = AppColors.Gold)
            }
        }
        adState != RewardedAdState.Unavailable -> RewardedButton(
            text = stringResource(if (adState == RewardedAdState.Ready) Res.string.ad_free_reward_watch else Res.string.ad_free_reward_loading),
            enabled = adState == RewardedAdState.Ready,
            onClick = {
                platform.logEvent(Events.REWARDED_AD)
                platform.showRewardedAd()
            },
        )
        // No fill or ads not initialised yet: keep a visible action instead of an empty card.
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RewardedButton(
                text = stringResource(Res.string.ad_free_reward_watch),
                enabled = true,
                onClick = { retried = true; platform.loadRewardedAd() },
            )
            if (retried) {
                Text(
                    stringResource(Res.string.ad_free_reward_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Gold-framed button with a pulsing play icon, so the free way to drop the ads catches the eye. */
@Composable
private fun RewardedButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.5.dp, if (enabled) AppColors.Gold else AppColors.Gold.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        AttentionPlayIcon(animate = enabled)
        Spacer(Modifier.width(14.dp))
        Text(text, color = AppColors.GoldLight, style = MaterialTheme.typography.titleSmall)
    }
}

/**
 * The play icon "breathes", sways a little and sends out two golden waves.
 * Animated values are read in graphicsLayer/drawBehind, so the animation only redraws the icon.
 */
@Composable
private fun AttentionPlayIcon(animate: Boolean) {
    if (!animate) {
        Icon(AppIcons.PlayCircle, contentDescription = null, tint = AppColors.Gold.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
        return
    }
    val transition = rememberInfiniteTransition()
    val wave by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2_000, easing = LinearEasing)))
    val breath by transition.animateFloat(1f, 1.12f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    val tilt by transition.animateFloat(-8f, 8f, infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    Box(
        Modifier.size(22.dp).drawBehind {
            val base = size.minDimension / 2f
            val glow = base * (1.4f + 2.5f * (breath - 1f))
            drawCircle(
                brush = Brush.radialGradient(listOf(AppColors.Gold.copy(alpha = 0.5f), AppColors.Gold.copy(alpha = 0f)), center = center, radius = glow),
                radius = glow,
            )
            // Two waves half a cycle apart: a calm, continuous pulse that fades out slowly.
            for (offset in listOf(0f, 0.5f)) {
                val p = (wave + offset) % 1f
                drawCircle(
                    color = AppColors.Gold.copy(alpha = sqrt(1f - p)),
                    radius = base * (1f + 0.75f * p),
                    style = Stroke(width = (2.5f - 1.5f * p).dp.toPx()),
                )
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            AppIcons.PlayCircle,
            contentDescription = null,
            tint = AppColors.GoldBright,
            modifier = Modifier.size(22.dp).graphicsLayer {
                scaleX = breath
                scaleY = breath
                rotationZ = tilt
            },
        )
    }
}

@Composable
private fun planLabel(plan: AdFreePlan): String {
    val trial = plan.freeTrial?.let { trialLength(it) }
    return when {
        plan.period == "P1Y" && trial != null -> stringResource(Res.string.ad_free_plan_yearly_trial, trial)
        plan.period == "P1Y" -> stringResource(Res.string.ad_free_plan_yearly)
        plan.period == "P1M" -> stringResource(Res.string.ad_free_plan_monthly)
        else -> plan.id
    }
}

/** "7 dni" for P7D or P1W, "1 miesiąc" for P1M; null for anything else. */
@Composable
private fun trialLength(period: String): String? {
    val match = Regex("""P(\d+)([DWM])""").matchEntire(period) ?: return null
    val n = match.groupValues[1].toInt()
    return when (match.groupValues[2]) {
        "D" -> pluralStringResource(Res.plurals.ad_free_days, n, n)
        "W" -> pluralStringResource(Res.plurals.ad_free_days, n * 7, n * 7)
        else -> pluralStringResource(Res.plurals.ad_free_months, n, n)
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.Gold,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

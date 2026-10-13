package droga_krzyzowa.droga_krzyzowa

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.russhwolf.settings.Settings
import droga_krzyzowa.droga_krzyzowa.data.AppLanguage
import droga_krzyzowa.droga_krzyzowa.data.AppPreferences
import droga_krzyzowa.droga_krzyzowa.data.Devotion
import droga_krzyzowa.droga_krzyzowa.data.NewsFeed
import droga_krzyzowa.droga_krzyzowa.platform.LocalAppLocale
import droga_krzyzowa.droga_krzyzowa.platform.LocalPlatformServices
import droga_krzyzowa.droga_krzyzowa.platform.PlatformServices
import droga_krzyzowa.droga_krzyzowa.platform.Screens
import droga_krzyzowa.droga_krzyzowa.platform.logView
import droga_krzyzowa.droga_krzyzowa.ui.components.AppBackground
import droga_krzyzowa.droga_krzyzowa.ui.components.LocalOpenAdFreeSettings
import droga_krzyzowa.droga_krzyzowa.ui.screens.DevotionScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.HomeScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.NewsScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.SettingsScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.SplashScreen
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppColors
import droga_krzyzowa.droga_krzyzowa.ui.theme.AppIcons
import droga_krzyzowa.droga_krzyzowa.ui.theme.WayOfTheCrossTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import wayofthecross_kmp.shared.generated.resources.Res
import wayofthecross_kmp.shared.generated.resources.notifications_allow
import wayofthecross_kmp.shared.generated.resources.notifications_later
import wayofthecross_kmp.shared.generated.resources.notifications_text
import wayofthecross_kmp.shared.generated.resources.notifications_title
import wayofthecross_kmp.shared.generated.resources.update_later
import wayofthecross_kmp.shared.generated.resources.update_ready_message
import wayofthecross_kmp.shared.generated.resources.update_ready_title
import wayofthecross_kmp.shared.generated.resources.update_restart

@Serializable object SplashRoute
@Serializable object HomeRoute
/** Keyed by [Devotion.key]: enum route arguments need a custom NavType on iOS. */
@Serializable data class DevotionRoute(val key: String) {
    constructor(devotion: Devotion) : this(devotion.key)

    val devotion: Devotion get() = Devotion.entries.first { it.key == key }
}
@Serializable object NewsRoute
@Serializable object SettingsRoute

val LocalPreferences = staticCompositionLocalOf<AppPreferences> { error("AppPreferences not provided") }
val LocalNewsFeed = staticCompositionLocalOf<NewsFeed> { error("NewsFeed not provided") }

private object AppSession {
    var startupDone = false
    var reopenSettings = false
    var updatePromptDismissed = false
}

@Composable
fun App(platform: PlatformServices, settings: Settings) {
    val preferences = remember {
        AppPreferences(settings).also { it.initLanguageIfNeeded(Locale.current.language) }
    }
    var language by remember { mutableStateOf(preferences.language) }
    val newsFeed = remember { NewsFeed(preferences) }
    LaunchedEffect(newsFeed) { newsFeed.refresh() }
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).components { add(KtorNetworkFetcherFactory()) }.build()
    }

    CompositionLocalProvider(
        LocalPlatformServices provides platform,
        LocalPreferences provides preferences,
        LocalNewsFeed provides newsFeed,
        LocalAppLocale provides language.code,
    ) {
        key(language) {
            WayOfTheCrossTheme {
                AppBackground {
                    AppNavHost(
                        language = language,
                        onLanguageChange = { newLanguage ->
                            if (newLanguage != language) {
                                preferences.language = newLanguage
                                AppSession.reopenSettings = true
                                language = newLanguage
                            }
                        },
                    )
                    UpdateReadyDialog()
                }
            }
        }
    }
}

@Composable
private fun UpdateReadyDialog() {
    val platform = LocalPlatformServices.current
    val updateReady by platform.updateReady.collectAsState()
    var dismissed by remember { mutableStateOf(AppSession.updatePromptDismissed) }
    if (!updateReady || dismissed) return

    AlertDialog(
        onDismissRequest = {
            AppSession.updatePromptDismissed = true
            dismissed = true
        },
        title = { Text(stringResource(Res.string.update_ready_title)) },
        text = { Text(stringResource(Res.string.update_ready_message)) },
        confirmButton = {
            TextButton(onClick = platform::completeUpdate) { Text(stringResource(Res.string.update_restart)) }
        },
        dismissButton = {
            TextButton(onClick = {
                AppSession.updatePromptDismissed = true
                dismissed = true
            }) { Text(stringResource(Res.string.update_later)) }
        },
    )
}

/** Asked once, after start-up, before the system permission dialog (news are sent as push notifications). */
@Composable
private fun NotificationPrompt() {
    val platform = LocalPlatformServices.current
    var visible by remember { mutableStateOf(platform.notificationPromptPending) }
    if (!visible) return

    fun answer(accepted: Boolean) {
        visible = false
        platform.answerNotificationPrompt(accepted)
    }

    AlertDialog(
        onDismissRequest = { answer(false) },
        icon = { Icon(AppIcons.Notifications, contentDescription = null, tint = AppColors.Gold) },
        title = { Text(stringResource(Res.string.notifications_title)) },
        text = { Text(stringResource(Res.string.notifications_text)) },
        confirmButton = {
            TextButton(onClick = { answer(true) }) { Text(stringResource(Res.string.notifications_allow)) }
        },
        dismissButton = {
            TextButton(onClick = { answer(false) }) { Text(stringResource(Res.string.notifications_later)) }
        },
    )
}

@Composable
private fun AppNavHost(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    val navController = rememberNavController()
    val startDestination: Any = if (AppSession.startupDone) HomeRoute else SplashRoute

    val platform = LocalPlatformServices.current
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            analyticsScreen(entry)?.let(platform::logView)
        }
    }

    LaunchedEffect(Unit) {
        if (AppSession.reopenSettings) {
            AppSession.reopenSettings = false
            navController.navigate(SettingsRoute)
        }
    }

    // From the news (opened in the settings) go back to those settings instead of stacking a second copy.
    val openAdFreeSettings: () -> Unit = remember(navController) {
        { if (!navController.popBackStack<SettingsRoute>(inclusive = false)) navController.navigate(SettingsRoute) }
    }
    CompositionLocalProvider(LocalOpenAdFreeSettings provides openAdFreeSettings) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { slideIntoContainer(SlideDirection.Start, tween(350)) + fadeIn(tween(350)) },
            exitTransition = { fadeOut(tween(250)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(350)) + fadeOut(tween(350)) },
        ) {
            composable<SplashRoute>(
                exitTransition = { fadeOut(tween(500)) },
            ) {
                SplashScreen(onFinished = {
                    AppSession.startupDone = true
                    navController.navigate(HomeRoute) { popUpTo<SplashRoute> { inclusive = true } }
                })
            }
            composable<HomeRoute>(
                enterTransition = { fadeIn(tween(500)) },
            ) {
                HomeScreen(
                    onDevotion = { navController.navigate(DevotionRoute(it)) },
                    onSettings = { navController.navigate(SettingsRoute) },
                )
                NotificationPrompt()
            }
            composable<DevotionRoute> { entry ->
                DevotionScreen(
                    devotion = entry.toRoute<DevotionRoute>().devotion,
                    language = language,
                    onBack = navController::popBackStackSafely,
                )
            }
            composable<NewsRoute> {
                NewsScreen(onBack = navController::popBackStackSafely)
            }
            composable<SettingsRoute> {
                // Already in the settings: no "Turn off ads" link over the banner here.
                CompositionLocalProvider(LocalOpenAdFreeSettings provides null) {
                    SettingsScreen(
                        language = language,
                        onLanguageChange = onLanguageChange,
                        onBack = navController::popBackStackSafely,
                        onNews = { navController.navigate(NewsRoute) },
                    )
                }
            }
        }
    }
}

private fun analyticsScreen(entry: NavBackStackEntry): String? {
    val destination = entry.destination
    return when {
        destination.hasRoute<HomeRoute>() -> Screens.HOME
        destination.hasRoute<DevotionRoute>() -> Screens.devotion(entry.toRoute<DevotionRoute>().devotion)
        destination.hasRoute<NewsRoute>() -> Screens.NEWS
        destination.hasRoute<SettingsRoute>() -> Screens.SETTINGS
        else -> null // The splash screen is not worth reporting.
    }
}

private fun NavHostController.popBackStackSafely() {
    if (previousBackStackEntry != null) popBackStack()
}

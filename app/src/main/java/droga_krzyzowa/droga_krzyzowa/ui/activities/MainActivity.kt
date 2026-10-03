package droga_krzyzowa.droga_krzyzowa.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import droga_krzyzowa.droga_krzyzowa.billing.SubscriptionManager
import droga_krzyzowa.droga_krzyzowa.billing.SubscriptionStatus
import droga_krzyzowa.droga_krzyzowa.ui.screens.AdBannerView
import droga_krzyzowa.droga_krzyzowa.ui.screens.DrogaKrzyzowaScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.GorzkieZaleScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.MainMenuScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.NewsFeedScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.NotificationPermissionDialog
import droga_krzyzowa.droga_krzyzowa.ui.screens.PrivacyPolicyScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.SettingsScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.SplashScreen
import droga_krzyzowa.droga_krzyzowa.ui.screens.SubscriptionScreen
import droga_krzyzowa.droga_krzyzowa.ui.theme.DeepPurple
import droga_krzyzowa.droga_krzyzowa.ui.theme.DrogaKrzyzowaTheme
import droga_krzyzowa.droga_krzyzowa.util.NotificationPrefs
import droga_krzyzowa.droga_krzyzowa.viewmodel.MainViewModel
import droga_krzyzowa.droga_krzyzowa.viewmodel.NewsViewModel
import androidx.core.net.toUri
import androidx.core.content.edit

// --- Nowe importy dla In-App Updates ---
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : androidx.appcompat.app.AppCompatActivity(), DefaultLifecycleObserver {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var mInterstitialAd: InterstitialAd? = null
    private var isShowingAd = false
    private lateinit var consentInformation: ConsentInformation
    private lateinit var mainViewModel: MainViewModel
    private var isLoadingAd = false

    private val showPermissionDialog = mutableStateOf(false)
    private lateinit var notifPrefs: NotificationPrefs
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.w("InAppUpdate", "Aktualizacja anulowana lub nie powiodła się. Kod: ${result.resultCode}")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notifPrefs.hasHandledPermissionRequest = true
        if (isGranted) {
            initFirebaseMessaging()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<androidx.appcompat.app.AppCompatActivity>.onCreate(savedInstanceState)

        notifPrefs = NotificationPrefs(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        MobileAds.initialize(this) {}
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        checkForUpdate()

        loadInterstitialAd()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )

        setContent {
            mainViewModel = viewModel()
            val mContext = LocalContext.current
            val subscriptionManager = remember { SubscriptionManager.getInstance(mContext) }
            val status by subscriptionManager.subscriptionStatus.observeAsState(SubscriptionStatus.CHECKING)

            LaunchedEffect(status) {
                when (status) {
                    SubscriptionStatus.PREMIUM -> {
                        mainViewModel.finishLoading()
                    }
                    SubscriptionStatus.NON_PREMIUM -> {
                        requestConsentAndLoadAds {
                            mainViewModel.finishLoading()
                        }
                    }
                    SubscriptionStatus.CHECKING -> { }
                }
            }

            LaunchedEffect(mainViewModel.isLoading.value) {
                if (!mainViewModel.isLoading.value) {
                    checkNotificationPermission()
                }
            }

            DrogaKrzyzowaTheme {
                if (mainViewModel.isLoading.value) {
                    SplashScreen()
                } else {
                    AppNavigation()
                }

                if (showPermissionDialog.value) {
                    NotificationPermissionDialog(
                        onDismiss = {
                            notifPrefs.hasHandledPermissionRequest = true
                            showPermissionDialog.value = false
                        },
                        onConfirm = {
                            showPermissionDialog.value = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        checkPromotionRedirect()

        // Sprawdzenie, czy proces wymuszonej aktualizacji nie został przerwany
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
        }
    }
    private fun checkForUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }

    private fun checkPromotionRedirect() {
        val sharedPref = getSharedPreferences("promotion_prefs", android.content.Context.MODE_PRIVATE)
        val storeUrl = sharedPref.getString("pending_store_url", null)

        if (storeUrl != null) {
            sharedPref.edit { remove("pending_store_url") }

            try {
                val intent = Intent(Intent.ACTION_VIEW, storeUrl.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=mivs.mojaparafia".toUri())
                startActivity(webIntent)
            }
        }
    }

    fun loadInterstitialAd() {
        if (mInterstitialAd != null || isLoadingAd) return

        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-8612826840770530/1459489385",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    isLoadingAd = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isLoadingAd = false
                }
            }
        )
    }

    fun showInterstitialAd() {
        if (mInterstitialAd != null && !isShowingAd) {
            isShowingAd = true

            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    isShowingAd = false
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    isShowingAd = false
                    loadInterstitialAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }

            mInterstitialAd?.show(this)
        } else {
            loadInterstitialAd()
        }
    }

    fun requestConsentAndLoadAds(onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                    if (consentInformation.canRequestAds()) {
                        onComplete()
                    } else {
                        onComplete()
                    }
                }
            },
            { requestConsentError ->
                onComplete()
            }
        )
    }

    private fun checkNotificationPermission() {
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (isGranted) {
            initFirebaseMessaging()
        } else if (!notifPrefs.hasHandledPermissionRequest) {
            showPermissionDialog.value = true
        }
    }

    private fun initFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_DK", "Pobieranie tokenu nie powiodło się", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_DK", "Token urządzenia Droga Krzyżowa: $token")
        }

        FirebaseMessaging.getInstance().subscribeToTopic("news_all")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_DK", "Zasubskrybowano temat: news_all")
                }
            }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val newsViewModel: NewsViewModel = viewModel()
    val configuration = LocalConfiguration.current
    val mContext = LocalContext.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val subscriptionManager = remember { SubscriptionManager.getInstance(mContext) }
    val isPremium by subscriptionManager.isPremium.observeAsState(false)

    Scaffold(
        containerColor = DeepPurple,
        bottomBar = {
            if (!isLandscape && !isPremium) {
                AdBannerView(onDisableAdsClick = { navController.navigate("subscription") })
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                ),
            color = DeepPurple
        ) {

            NavHost(navController = navController, startDestination = "menu") {
                composable("menu") {
                    MainMenuScreen(
                        navController = navController,
                        newsViewModel = newsViewModel,
                        onNavigateToWayOfCross = { navController.navigate("droga_krzyzowa") },
                        onNavigateToGorzkieZale = { navController.navigate("gorzkie_zale") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToSubscription = { navController.navigate("subscription") },
                        onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") }
                    )
                }

                composable("droga_krzyzowa") {
                    DrogaKrzyzowaScreen(
                        navController = navController,
                        onShowAd = {
                            (navController.context as? MainActivity)?.showInterstitialAd()
                        }
                    )
                }

                composable("gorzkie_zale") {
                    GorzkieZaleScreen(
                        navController = navController,
                        onShowAd = {
                            (navController.context as? MainActivity)?.showInterstitialAd()
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        navController = navController,
                        onNavigateToPrivacyPolicy = {
                            navController.navigate("privacy_policy")
                        }
                    )
                }

                composable("subscription") {
                    SubscriptionScreen(navController = navController)
                }
                composable("privacy_policy") {
                    PrivacyPolicyScreen(navController)
                }
                composable("news_feed") {
                    NewsFeedScreen(navController = navController, viewModel = newsViewModel)
                }
            }
        }
    }
}

@Composable
fun TrackScreenView(screenName: String) {
    val context = LocalContext.current
    val analytics = remember { FirebaseAnalytics.getInstance(context) }

    DisposableEffect(Unit) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)

        onDispose { }
    }
}
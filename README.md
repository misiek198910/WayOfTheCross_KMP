This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Local configuration (not in the repository)

- `local.properties`:
  ```properties
  sdk.dir=/path/to/Android/sdk
  # release signing
  release.storeFile=/path/to/keystore.jks
  release.storePassword=...
  release.keyAlias=...
  release.keyPassword=...
  # AdMob (release builds; debug builds use Google's test ids). Android shows the app open ad and the anchored adaptive banner.
  admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
  admob.bannerId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
  admob.appOpenId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
  # "watch an ad for 24 h without ads" is only offered when set
  admob.rewardedId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
  ```
- Firebase: `androidApp/google-services.json` (Analytics and the `news_all` push topic) and
  `iosApp/iosApp/GoogleService-Info.plist` (Analytics). Without them the app runs without Firebase.
- `iosApp/Configuration/Secrets.xcconfig` (AdMob for iOS release builds; without it the app builds without ads):
  ```
  ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
  ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
  ADMOB_APP_OPEN_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
  ADMOB_REWARDED_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
  ```

### Ads and the ad-free option

- Android: app open ad and anchored adaptive banner; ads are removed with the Google Play subscription `remove_ads_year`
  or for 24 hours with a rewarded ad.
- iOS: app open ad, anchored adaptive banner, and a rewarded ad that turns ads off for 24 hours. No subscription yet
  (`ContentView.swift` passes `nil` for `IosSubscriptions`).

### Analytics

Every view is logged as `screen_view` and as an event of the same name, e.g. `DrogaKrzyzowa_ustawienia`,
`DrogaKrzyzowa_stacja_7`; actions in the "Remove ads" card as `DrogaKrzyzowa_subskrypcje_*` and
`DrogaKrzyzowa_reklama_z_nagroda`. All names are in `shared/.../platform/Analytics.kt`.

### Content

- Station and Bitter Lamentations texts: `shared/src/commonMain/composeResources/files/{station,lamentations}_<n>_<pl|en>.html`
  (a small HTML subset: `b`, `i`, `big`, `br`).
- Pictures: `composeResources/drawable/station_<n>.webp`, `station_<n>_land.webp` and `lamentations_<n>.webp`.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
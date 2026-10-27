# SowarApp · صور اليوم

An Arabic-first Android app for browsing greeting images (صباح الخير، مساء الخير، ورود، حكم وعبر، جمعة مباركة، مناسبات), then downloading or sharing them.

**Phase 1: UI foundation.** Everything runs on the phone: no backend, Firebase, accounts, analytics or ads yet.

## Features in this version

| Feature | Status |
|---|---|
| Home: title, search, category cards, popular images | Done |
| Category screen with a responsive grid and empty state | Done |
| Image details screen | Done |
| Download: saves a JPEG to `Pictures/SowarApp` in the gallery | Done |
| Share: opens Android's share sheet (WhatsApp, Telegram…) with the image | Done |
| Favorites saved on the phone (survive restarts) | Done |
| Bottom navigation (Home / Favorites) | Done |
| Right-to-left Arabic layout, even if the phone is in English | Done |
| Real photo catalog from the internet | Later phase |
| AdMob, publishing to Google Play | Later phase |

All 30 sample images were generated for this project by a script (gradients, flowers, sun, moon, patterns), so there are no copyright issues. The Arabic text is drawn by the app on top of each image, and the same text is written into the downloaded or shared file.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM · Navigation Compose · DataStore · Gradle Kotlin DSL
`minSdk 24` (Android 7.0+), `targetSdk 35`.

## Project structure

```
app/src/main/java/com/sowarapp/android/
├── MainActivity.kt          Starts the app and forces RTL
├── SowarApplication.kt      Creates AppContainer once
├── AppContainer.kt          Builds the repositories (simple manual dependency injection)
├── data/
│   ├── model/               Category, SowarImage
│   ├── LocalImageDataSource The built-in catalog (later: replace with a remote source)
│   ├── ImageRepository      Categories, images, Arabic-friendly search
│   ├── FavoritesRepository  Saves favorite ids with DataStore
│   ├── ImageExporter        Draws caption on image, saves to gallery, prepares share files
│   └── ArabicText           Normalises Arabic for search (أ/ا, ة/ه, diacritics)
└── ui/
    ├── SowarApp.kt          Scaffold + bottom navigation bar
    ├── AppViewModelProvider Builds every ViewModel
    ├── navigation/          Routes and the NavHost
    ├── components/          Reusable pieces: cards, grid, empty state, top bar
    ├── home/ category/ details/ favorites/   One folder per screen (Screen + ViewModel)
    └── theme/               Colors, typography
```

## How to run it on your phone

### Option A: install the ready-made APK (no Android Studio needed)
1. Open the repository on GitHub → **Actions** tab → the latest green **Android build** run.
2. Download the **SowarApp-debug-apk** artifact and unzip it.
3. Copy `app-debug.apk` to your phone and open it. Android will ask you to allow installing from this source; allow it.

### Option B: Android Studio
1. Install [Android Studio](https://developer.android.com/studio) (latest stable).
2. **File → Open…** and pick this project folder (the one containing `settings.gradle.kts`).
3. Wait for "Gradle sync" to finish (first time downloads a few hundred MB).
4. On your phone: **Settings → About phone →** tap **Build number** 7 times to enable Developer options. Then **Settings → Developer options →** turn on **USB debugging**.
5. Connect the phone with a USB cable and accept the "Allow USB debugging?" prompt.
6. Choose your phone in the device menu at the top of Android Studio and press the green **Run ▶** button.

No phone? Use **Device Manager → Create device** to make an emulator, then press Run.

### Command line
```bash
./gradlew assembleDebug        # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # runs unit tests
./gradlew lintDebug            # Android lint
```

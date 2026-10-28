# SowarApp · صور اليوم

An Arabic-first Android app for browsing greeting images (صباح الخير، مساء الخير، ورود، حكم وعبر، جمعة مباركة، مناسبات), then downloading or sharing them.

**Phase 2: online catalog.** Images now load from the internet, using a catalog file. There is still no backend, Firebase, accounts, analytics or ads.

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
| Photos loaded from an online catalog, cached on the phone | Done |
| Captions optional per image (for images that already have text) | Done |
| Photo credit (author and license) on the details screen | Done |
| Hosting for ~10,000 images, videos | Later phase |
| AdMob, publishing to Google Play | Later phase |

## Where the images come from

The app reads [`catalog/catalog.json`](catalog/catalog.json): the list of categories and images, with a link to each photo.

- **Online:** the app downloads the newest catalog from `main` on GitHub each time it starts (`CATALOG_URL` in `app/build.gradle.kts`).
- **Offline:** it keeps the last downloaded copy on the phone, and the APK also includes a copy, so the first launch works without internet. The photos themselves still need internet the first time, and after that they come from Coil's cache.

**Test images for now.** [`tools/build_test_catalog.py`](tools/build_test_catalog.py) picks photos from Wikimedia Commons "Quality images" whose license allows app use with credit (CC0, public domain, CC BY, then CC BY-SA if needed). The app shows the author and license under each photo. GitHub Actions runs the script and commits the result (**Actions → Build test catalog → Run workflow**). These images are only for testing and will be replaced by your own.

**Catalog format** (one entry in `items`):

```json
{
  "id": "flowers_1", "categoryId": "flowers", "type": "image",
  "url": "https://…/1280px-rose.jpg", "thumbnailUrl": "https://…/500px-rose.jpg",
  "width": 1280, "height": 1707,
  "caption": "باقة ورد لك", "showCaption": true, "tags": ["ورود"], "popular": false,
  "credit": { "author": "…", "license": "CC BY 4.0", "sourceUrl": "https://commons.wikimedia.org/…" }
}
```

- `showCaption: false` for images that already have text on them. The app then shows the photo as it is.
- `type` is `image` for now. Entries with other types (for example a future `video`) are skipped by this version, so adding them later won't break installed apps.
- Unknown extra fields are ignored, so the format can grow.

**Image hosting: Cloudflare R2.** The photos are served from an R2 bucket on our own domain (downloads from R2 are free). [`tools/copy_images_to_r2.py`](tools/copy_images_to_r2.py) downloads each photo in the catalog, makes a WebP full image (1280 px wide at most) and a 500 px thumbnail, uploads both to `images/<category>/<id>.webp` and `thumbs/<category>/<id>.webp`, and rewrites the links in the catalog. Photos already on the domain are skipped. Run it with **Actions → Copy images to R2 → Run workflow**, giving the image domain. It needs the repo secrets `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID` and `R2_SECRET_ACCESS_KEY`.

**Adding your own photos** (no app update needed):

1. In Cloudflare: **R2 → sowarapp-images → Upload**. Put each photo in a folder named after its category: `inbox/flowers/`, `inbox/good_morning/`, `inbox/good_evening/`, `inbox/wisdom/`, `inbox/jumuah/` or `inbox/occasions/`.
2. On GitHub: **Actions → Add new images → Run workflow**.
3. Open the app again. The new photos show at the top of their category.

[`tools/add_new_images.py`](tools/add_new_images.py) resizes each photo, uploads it next to the others, adds it to the catalog without a caption (most photos already have text) and empties the inbox. Uploading the same photo twice adds it once. "Build test catalog" now runs only by hand, because it would replace these photos.

At 10,000 images the single catalog file gets too big, so a later step will split it into pages.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM · Navigation Compose · DataStore · Coil 3 (images) · OkHttp · kotlinx.serialization · Gradle Kotlin DSL
`minSdk 24` (Android 7.0+), `targetSdk 35`.

## Project structure

```
app/src/main/java/com/sowarapp/android/
├── MainActivity.kt          Starts the app and forces RTL
├── SowarApplication.kt      Creates AppContainer once
├── AppContainer.kt          Builds the repositories (simple manual dependency injection)
├── data/
│   ├── model/               Category, SowarImage, Catalog
│   ├── model/Catalog        Lookups and Arabic-friendly search over the catalog
│   ├── CatalogDataSource    Downloads catalog.json; falls back to the saved or bundled copy
│   ├── CatalogParser        Reads catalog.json (ignores unknown fields and types)
│   ├── ImageRepository      Holds the current catalog; screens observe it
│   ├── FavoritesRepository  Saves favorite ids with DataStore
│   ├── ImageExporter        Gets the photo via Coil, draws the caption, saves or shares it
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

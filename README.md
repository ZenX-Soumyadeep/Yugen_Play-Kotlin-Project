<div align="center">

# 🌌 YugenPlay

**A modern, production-grade Android anime streaming and tracking client.**  
*Engineered with 100% Jetpack Compose, AndroidX Media3 (ExoPlayer), Clean Architecture, and AniList GraphQL Sync.*

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.08.00-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android SDK](https://img.shields.io/badge/API-29%20to%2037-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Media3](https://img.shields.io/badge/Media3%20ExoPlayer-1.11.0-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://developer.android.com/media/media3)
[![Room](https://img.shields.io/badge/Room-2.8.4-33B5E5?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Hilt](https://img.shields.io/badge/Hilt%20DI-2.60.1-4CAF50?style=for-the-badge&logo=google&logoColor=white)](https://dagger.dev/hilt/)
[![License](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)

---

</div>

## 📖 Overview

**YugenPlay** is a high-performance native Android application tailored for anime enthusiasts. Designed from the ground up to eliminate compromises, it combines a **glassmorphic 120Hz micro-animated UI**, an **advanced Media3 ExoPlayer engine** with multi-track VTT styling, **resilient background batch downloading**, and **two-way AniList GraphQL synchronization**.

Whether streaming adaptive HLS over unstable networks, casting via Google Cast proxy, customizing subtitle edges and opacities, or caching whole seasons for offline flights, YugenPlay provides an uninterrupted, fluid experience.

---

## ✨ Key Features

### 🎬 Advanced Playback Engine (Media3 ExoPlayer)
* **Adaptive HLS & MP4 Streaming:** Automatic bitrate and resolution switching, custom referer/origin header forwarding, and hardware-accelerated rendering.
* **Precision Playback Speeds:** Adjust speed from `0.25×` to `2.0×` with immediate audio-pitch-corrected playback and top-bar HUD indicators.
* **Granular Subtitle Engine:** Customize subtitle font size (`Small`, `Normal`, `Large`), edge styling (`Drop Shadow`, `Outline`, `Box`), text color palette, and background opacity (`0%`, `40%`, `75%`) dynamically without restarting the player.
* **Fluid Scrubbing & Dual-Axis Gestures:** Vertical edge swipes for volume and brightness; horizontal drag for precision scrubbing; double-tap seek ripples with user-configurable intervals (`5s` to `30s`).
* **Auto-Play & Auto-Skip:** Configurable countdown timer for next episode auto-play; seamless intro/outro skipping markers.
* **Picture-in-Picture (PiP) & In-Player Orientation:** Native Android PiP mode alongside a dedicated one-tap landscape/portrait toggle with adaptive control density.
* **Google Cast:** Built-in Chromecast support with a local secure proxy bridge for authenticated streams.

### 📥 Offline Batch Downloads & Storage Management
* **Foreground Service Architecture:** Powered by `androidx.media3.exoplayer.offline.DownloadService` with `dataSync` foreground protection against aggressive OEM memory killer daemons. Displays grouped InboxStyle notifications dynamically updating progress for each anime.
* **Batch Downloader Sheet:** Select all episodes, unwatched only, or specific ranges to download an entire season concurrently. All batch queued episodes are strictly queued in numeric order.
* **Subtitle Packaging:** Downloads remote `.vtt` subtitles locally and packs them into encrypted JSON metadata payloads attached directly to ExoPlayer download records.
* **Live Storage Metrics:** Real-time pill badge tracking offline library disk footprint alongside free device storage.
* **Full Queue Control:** Pause, resume, retry, cancel, or clear all downloads with swipe-to-dismiss actions. Features confirmation dialogs for grouped and single episode deletions to prevent accidental data loss.

### 🔄 AniList Cloud Synchronization
* **Two-Way Status Tracking:** Instant sync with your AniList account (Watching, Completed, Planning, Paused, Dropped).
* **Episode Progress Sync:** Automatically marks watched episodes as you finish playback or trigger manual progression.
* **Airing Alerts & Notifications:** Real-time polling for upcoming episodes and airing broadcast schedules with unread badges.
* **Seamless Authentication:** Secure OAuth2 flow via deep link scheme (`yugenplay://auth`).

### 🔍 Discovery & Rich Metadata
* **Smart Filterable Search:** Multi-dimensional querying by format (`TV`, `MOVIE`, `OVA`, `ONA`), season, release year, genres, and sort orders.
* **Instant Tag Navigation:** Tap any genre tag on anime details or "View All" on trending sections to immediately jump into pre-filtered search results.
* **AniZip & TMDB Integration:** Automatic episode thumbnail normalization and synopsis enrichment for crystal-clear browsing.

### ⚙️ Persistent User Preferences
* **Jetpack DataStore:** Double-tap seek duration, default dub vs sub server priority, subtitle styling presets, playback speed, and maximum parallel batch download limits persist seamlessly across device reboots.
* **"What's New" Sheet:** Version-aware modal automatically highlighting new features on upgrade, accessible anytime from Settings.

---

## 🏛 Architecture & Tech Stack

The codebase follows **Clean Architecture** principles structured into three distinct layers with **Unidirectional Data Flow (UDF)**:

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Presentation)              │
│   • Jetpack Compose & Material 3 Components            │
│   • ViewModels with StateFlow (UDF pattern)            │
│   • Responsive Glassmorphic Layouts & Animations       │
└───────────────────────────▲────────────────────────────┘
                            │ Emits UI State / Dispatches Events
┌───────────────────────────┴────────────────────────────┐
│                    Domain Layer (Business)             │
│   • Use Cases (GetVideoStreams, GetEpisodes, Sync)     │
│   • Pure Kotlin Domain Models & Repository Contracts   │
└───────────────────────────▲────────────────────────────┘
                            │ Consumes Repositories / Sources
┌───────────────────────────┴────────────────────────────┐
│                     Data Layer (Data)                  │
│   • Remote: Retrofit, OkHttp, AniList GraphQL, AniZip  │
│   • Local: Room Database, Jetpack DataStore Preferences│
│   • Services: Media3 VideoDownloadService, WorkManager │
└────────────────────────────────────────────────────────┘
```

| Technology | Purpose | Version |
| :--- | :--- | :--- |
| **Kotlin** | Modern, concise, expressive programming language | `2.4.10` |
| **Jetpack Compose** | Declarative, modern UI toolkit with Material 3 | `2026.08.00` (BOM) |
| **Media3 ExoPlayer** | Audio/video playback, HLS handling, and offline downloads | `1.11.0` |
| **Dagger Hilt** | Dependency injection across Activities, ViewModels, and Workers | `2.60.1` |
| **Room Database** | Offline persistence for watch history and favorites | `2.8.4` |
| **DataStore** | Type-safe, reactive key-value storage for player settings | `1.2.1` |
| **Retrofit & OkHttp**| Resilient HTTP networking with interceptors & connection pools | `3.0.0` / `5.5.0` |
| **Coil** | Asynchronous image loading with memory & disk cache | `2.7.0` |
| **WorkManager** | Guaranteed background synchronization tasks | `2.11.2` |

---

## 🔬 Core Functions & Architectural Highlights

Below are in-depth technical breakdowns of critical functions within the application, accompanied by comprehensive inline comments.

### 1. In-Memory Stream Caching & Batch Download Resolution
Located in [`DetailViewModel.kt`](app/src/main/java/com/zenx/yugen/play/ui/detail/DetailViewModel.kt), this mechanism prevents network exhaustion and redundant scraping during multi-episode download queueing.

```kotlin
/**
 * Resolves video streams for multiple episodes sequentially, preferring cached
 * stream URLs where available, and queues eligible episodes for offline download.
 *
 * @param episodes List of episode UI models selected by the user.
 * @param preferDub When true, prioritizes English dub streams over default sub streams.
 */
fun batchDownloadEpisodes(episodes: List<EpisodeUiModel>, preferDub: Boolean) {
    if (episodes.isEmpty()) return
    hideBatchDownloadSheet()

    viewModelScope.launch {
        // Step 1: Filter out episodes already queued, preparing, or downloaded
        val toDownload = episodes.filter {
            it.downloadState != DownloadState.COMPLETED &&
            it.downloadState != DownloadState.DOWNLOADING &&
            !it.isPreparing
        }
        if (toDownload.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "All selected episodes are already downloaded.", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Queueing ${toDownload.size} episodes for download...", Toast.LENGTH_SHORT).show()
        }

        var queuedCount = 0
        for (ep in toDownload) {
            // Step 2: Query LruCache for stream resolution; fallback to remote scrape on miss
            val cached = StreamDataCache.get(ep.id)
            val streams = if (cached != null) {
                cached
            } else {
                val res = getVideoStreamsUseCase(ep.id)
                if (res is Resource.Success) {
                    res.data?.also { StreamDataCache.set(ep.id, it) } ?: emptyList()
                } else emptyList()
            }

            // Step 3: Match preferred stream type (Dub vs Sub)
            if (streams.isNotEmpty()) {
                val stream = if (preferDub) {
                    streams.find { it.serverName?.contains("dub", ignoreCase = true) == true || it.quality.contains("dub", ignoreCase = true) }
                        ?: streams.first()
                } else {
                    streams.find { it.serverName?.contains("dub", ignoreCase = true) != true && !it.quality.contains("dub", ignoreCase = true) }
                        ?: streams.first()
                }
                
                // Step 4: Dispatch to download worker pipeline
                enqueueDownload(ep, stream)
                queuedCount++
            }
        }

        // Step 5: Notify user of overall queueing success
        withContext(Dispatchers.Main) {
            if (queuedCount > 0) {
                Toast.makeText(context, "Queued $queuedCount episodes for download.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to resolve streams for selected episodes.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

---

### 2. Reactive Subtitle Engine & ExoPlayer View Configuration
Located in [`PlayerScreen.kt`](app/src/main/java/com/zenx/yugen/play/ui/player/PlayerScreen.kt), this handles dynamic typography, alpha compositing, and edge styles on the active ExoPlayer `SubtitleView` without interrupting playback.

```kotlin
// AndroidView update callback executing reactively whenever PlayerUiState changes
update = { view ->
    val readyState = uiState as? PlayerUiState.Ready

    // Update aspect ratio resize mode
    view.resizeMode = when (readyState?.resizeMode) {
        VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        VideoResizeMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    readyState?.let { state ->
        view.subtitleView?.apply {
            // Apply proportional fractional text size based on player dimensions
            setFractionalTextSize(state.subtitleSize)

            // Convert 64-bit Long color to standard 32-bit ARGB
            val textArgb = state.subtitleTextColor.toInt()
            
            // Compute translucent background window color using user's opacity preference
            val bgColor = android.graphics.Color.argb(
                (state.subtitleBgOpacity * 255).toInt(), 
                0, 0, 0
            )
            
            // Map edge style constant (None, Outline, Drop Shadow, Box)
            val edgeColor = when (state.subtitleEdgeStyle) {
                1 -> android.graphics.Color.BLACK                     // Outline
                2 -> android.graphics.Color.parseColor("#80000000")   // Drop Shadow
                else -> android.graphics.Color.TRANSPARENT            // Box / None
            }

            // Construct and bind CaptionStyleCompat to ExoPlayer SubtitleView
            setStyle(
                CaptionStyleCompat(
                    textArgb,
                    bgColor,
                    android.graphics.Color.TRANSPARENT,
                    @Suppress("WrongConstant") state.subtitleEdgeStyle,
                    edgeColor,
                    null // Default system typeface
                )
            )
        }
    }
}
```

---

### 3. Metadata Serialization for Media3 Offline Downloads
Located in [`DetailViewModel.kt`](app/src/main/java/com/zenx/yugen/play/ui/detail/DetailViewModel.kt), this serializes anime titles, headers, local subtitle file URIs, and skip markers into a compact byte payload attached directly to `DownloadRequest`.

```kotlin
/**
 * Constructs an optimized JSON payload string converted to a UTF-8 byte array,
 * encapsulating all stream metadata required to play back an episode fully offline.
 */
private fun buildOptimizedMetadataPayload(
    animeTitle: String,
    episodeNumber: String,
    episodeTitle: String,
    posterUrl: String,
    subtitles: List<JSONObject>,
    headers: JSONObject,
    skipIntervals: List<JSONObject>
): MetadataPayloadResult {
    val fullJson = JSONObject().apply {
        put("animeTitle", animeTitle)
        put("episodeNumber", episodeNumber)
        put("episodeTitle", episodeTitle)
        put("posterUrl", posterUrl)
        put("subtitles", JSONArray(subtitles))
        put("headers", headers)
        put("skipIntervals", JSONArray(skipIntervals))
    }

    var payloadStr = fullJson.toString()
    var wasSubtitlesTruncated = false

    // Guard against Media3 SQLite metadata byte size limits (coerced under 60KB)
    if (payloadStr.toByteArray(Charsets.UTF_8).size > 60_000) {
        wasSubtitlesTruncated = true
        // Retain only primary/default subtitle to ensure metadata persists safely
        val defaultSub = subtitles.firstOrNull { it.optBoolean("isDefault") } 
            ?: subtitles.firstOrNull()
        fullJson.put("subtitles", JSONArray(listOfNotNull(defaultSub)))
        payloadStr = fullJson.toString()
    }

    return MetadataPayloadResult(
        data = payloadStr.toByteArray(Charsets.UTF_8),
        wasSubtitlesTruncated = wasSubtitlesTruncated
    )
}
```

---

### 4. AniZip & TMDB Episode Metadata Normalization
Located in [`EpisodeMetadataService.kt`](app/src/main/java/com/zenx/yugen/play/data/remote/EpisodeMetadataService.kt), this service scrapes episode descriptions and resolves relative TMDB image paths to absolute CDN endpoints.

```kotlin
/**
 * Scrapes and normalizes AniZip metadata by AniList media ID.
 * Resolves multilingual titles, synopsis, and relative TMDB image paths.
 */
suspend fun getMetadata(anilistId: Int): Map<Int, ExternalEpisodeMeta> = withContext(Dispatchers.IO) {
    // Return cached map if already queried during session
    val cached = cache[anilistId]
    if (!cached.isNullOrEmpty()) return@withContext cached

    val result = mutableMapOf<Int, ExternalEpisodeMeta>()
    val jsonString = fetchWithRetry("$ANIZIP_BASE_URL$anilistId") ?: return@withContext emptyMap()

    try {
        val json = JSONObject(jsonString)
        val episodesObj = json.optJSONObject("episodes")
        
        if (episodesObj != null) {
            val keys = episodesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val epNode = episodesObj.optJSONObject(key) ?: continue
                val epNum = epNode.optInt("episodeNumber", epNode.optInt("episode", key.toIntOrNull() ?: -1))

                if (epNum != -1) {
                    // Title normalization: fallback English -> Romanized -> Japanese
                    val titleNode = epNode.opt("title")
                    val title = when (titleNode) {
                        is JSONObject -> titleNode.optString("en").ifBlank {
                            titleNode.optString("x-jat").ifBlank { titleNode.optString("ja", "") }
                        }
                        is String -> titleNode
                        else -> ""
                    }.trim()

                    val desc = epNode.optString("overview", epNode.optString("description", "")).trim()

                    // Image extraction: cascade through possible thumbnail keys
                    val rawImage = epNode.optString("image").ifBlank {
                        epNode.optString("thumbnail").ifBlank {
                            epNode.optString("thumb").ifBlank {
                                epNode.optString("picture", "")
                            }
                        }
                    }.trim()

                    // Normalize relative TMDB image references into absolute high-res CDN URLs
                    val image = when {
                        rawImage.startsWith("http://") || rawImage.startsWith("https://") -> rawImage
                        rawImage.startsWith("/") -> "https://image.tmdb.org/t/p/w500$rawImage"
                        else -> rawImage
                    }

                    result[epNum] = ExternalEpisodeMeta(epNum, title, desc, image)
                }
            }
        }
        if (result.isNotEmpty()) cache[anilistId] = result
    } catch (e: Exception) {
        Log.e(TAG, "Failed parsing AniZip metadata response for ID: $anilistId", e)
    }

    return@withContext result
}
```

---

## 🛠️ Environment Setup & Prerequisites

Before cloning and building YugenPlay, verify your development environment meets the following specifications:

| Requirement | Minimum / Recommended |
| :--- | :--- |
| **Operating System** | Linux, macOS (Apple Silicon/Intel), or Windows 10/11 |
| **JDK (Java Development Kit)** | **Java 21** (e.g. Azul Zulu JDK 21 or OpenJDK 21) |
| **Android Studio** | **Koala Feature Drop (2024.1.2)**, **Ladybug (2024.2.1+)**, or newer |
| **Android SDK Platform** | **API Level 37** (Android 15+) |
| **Android Build-Tools** | **37.0.0** |
| **Minimum Device OS** | **Android 10.0 (API Level 29)** |
| **Target Device OS** | **Android 15.0 (API Level 37)** |

---

## 🚀 Quickstart Commands

### 1. Clone the Repository
```bash
git clone https://github.com/ZenX-Soumyadeep/Yugen.git
cd Yugen
```

### 2. Verify Kotlin Compilation
Run Kotlin incremental compilation to ensure all KSP processors, Compose compiler plugins, and Hilt modules compile cleanly:
```bash
./gradlew compileDebugKotlin
```

### 3. Assemble Debug APK
Compile and bundle the debug APK with full debugging symbols:
```bash
./gradlew assembleDebug
```
The output artifact will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Install Directly to Connected Device / Emulator
Ensure ADB detects your device (`adb devices`), then execute:
```bash
./gradlew installDebug
```

### 5. Run Unit Tests & Linting
Execute all local JUnit 4 unit tests across ViewModels, Use Cases, and Repositories:
```bash
./gradlew testDebugUnitTest
```

### 6. Assemble Production Release Bundle
Build the optimized, minified (R8 / ProGuard enabled) production APK:
```bash
./gradlew assembleRelease
```
The release artifact will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📂 Project Directory Structure

```text
com.zenx.yugen.play
├── data
│   ├── local/               # Room entities, DAOs (WatchHistory, Favorites), DataStore preferences
│   ├── remote/              # Retrofit endpoints, OkHttp setup, AniList GraphQL, AniZip service
│   └── repository/          # Concrete implementations of domain repository interfaces
├── domain
│   ├── usecase/             # Clean business use cases (GetVideoStreams, GetEpisodes, Sync, etc.)
│   └── Models.kt            # Core entity models (AnimeCardItem, VideoStream, Subtitle, etc.)
├── service
│   ├── VideoDownloadService.kt # Media3 foreground download execution service
│   ├── DownloadTracker.kt      # Real-time state tracker for active/completed offline files
│   └── CastProxyService.kt     # Local HTTP proxy bridge for Chromecast media delivery
├── ui
│   ├── home/                # Home dashboard, Hero carousel, Continue Watching section
│   ├── detail/              # Anime info, episode list, batch download bottom sheet
│   ├── player/              # ExoPlayer surface, gestures, HUDs, and side control panels
│   ├── downloads/           # Offline downloads manager, storage meter, queue controllers
│   ├── search/              # Advanced search, tag filtering, and instant pre-population
│   ├── library/             # AniList categories (Watching, Planning, Completed, etc.)
│   ├── settings/            # Player preferences, cache cleaner, and version diagnostic cards
│   ├── components/          # Reusable Compose widgets (WhatsNewBottomSheet, Modifiers, Chips)
│   └── MainScreen.kt        # Root scaffold with bottom navigation and Compose NavHost
└── util
    ├── SystemNotificationHelper.kt # Channel managers & airing broadcast push notifications
    ├── StringUtils.kt             # Title sanitizers and regex season parsers
    └── CastOptionsProvider.kt     # Google Cast framework configuration
```

---

## 🤝 Contributing

Contributions, feature suggestions, and bug reports are warmly welcomed!

1. **Fork the Project** on GitHub.
2. **Create a Feature Branch** (`git checkout -b feature/AmazingFeature`).
3. **Commit your Changes** (`git commit -m 'Add some AmazingFeature'`).
4. **Push to the Branch** (`git push origin feature/AmazingFeature`).
5. **Open a Pull Request**.

Please ensure your changes pass `./gradlew compileDebugKotlin` and `./gradlew testDebugUnitTest` before opening a pull request.

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for complete details.

---

<div align="center">

Crafted with ❤️ by **[ZenX-Soumyadeep](https://github.com/ZenX-Soumyadeep)** and contributors.

</div>

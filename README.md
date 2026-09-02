# Yugen Play 

A premium, native Android anime streaming and tracking application. Built entirely with Jetpack Compose, featuring a fluid Dynamic Island UX, background downloading, and seamless AniList integration.

## 🚀 Features
* **Native Compose UI:** Smooth 120Hz animations, translucent overlays, and an Apple-inspired Dynamic Island stream selector.
* **ExoPlayer Integration:** Adaptive HLS streaming, multi-track subtitle selection (.vtt), and intro/outro skipping.
* **Background Downloads:** Media3 offline downloading service with persistent storage management.
* **AniList Sync:** Two-way synchronization for watch history, library tracking, and offline progress resolution.
* **Google Cast:** Proxy-enabled Chromecast support.

## 🛠 Tech Stack
* **UI:** Jetpack Compose, Material 3
* **Media:** AndroidX Media3 (ExoPlayer, Cast, Session, Offline)
* **Architecture:** MVVM, Clean Architecture, Hilt (Dependency Injection)
* **Networking/Data:** Retrofit, OkHttp, Room Database, Jsoup
* **Background Processing:** WorkManager

## 🔐 Getting Started
1. Clone the repository: `git clone https://github.com/ZenX-Soumyadeep/Yugen`
2. Open the project in **Android Studio Koala** (or newer).
3. Create a `local.properties` file in the root directory and add your AniList API credentials:
   ```properties
   ANILIST_CLIENT_ID="your_client_id"
   ANILIST_CLIENT_SECRET="your_client_secret"

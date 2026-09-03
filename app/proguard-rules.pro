# --- Kotlin & Coroutines ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn kotlinx.coroutines.**

# --- Data Domain & Serialization Models ---
# Keeps all data and domain models from being mangled so JSON serialization works
-keep class com.zenx.yugen.play.domain.** { *; }
-keep class com.zenx.yugen.play.data.remote.** { *; }
-keep class com.zenx.yugen.play.data.local.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# --- Room Persistence Library ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# --- AndroidX WorkManager & Hilt Work ---
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.hilt.work.WorkerAssistedFactory { *; }

# --- AndroidX Media3 (ExoPlayer, HLS & Cast) ---
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.cast.** { *; }
-dontwarn androidx.media3.**

# --- Google Play Services Cast Framework ---
-keep class com.zenx.yugen.play.util.CastOptionsProvider { *; }
-keep class com.google.android.gms.cast.framework.** { *; }
-keep class * extends com.google.android.gms.cast.framework.OptionsProvider

# --- OkHttp & Okio ---
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class okhttp3.OkHttpClient {
    okhttp3.Call newCall(okhttp3.Request);
}
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Jsoup DOM Parser ---
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# --- AndroidX Security Crypto ---
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**
package com.zenx.yugen.play.ui.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

data class AppUpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tag = "UpdateViewModel"

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    companion object {
        private var cachedUpdateInfo: AppUpdateInfo? = null
        private var lastCheckTime: Long = 0L
        // 4.6: 6-hour TTL to guard GitHub API rate-limits
        private const val UPDATE_CHECK_TTL_MS = 6 * 60 * 60 * 1000L
    }

    init {
        checkForUpdates()
    }

    fun checkForUpdates(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastCheckTime) < UPDATE_CHECK_TTL_MS && lastCheckTime > 0L) {
            _updateInfo.value = cachedUpdateInfo
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val body = okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@launch
                    response.body?.string() ?: return@launch
                }

                val json = JSONObject(body)
                val latestVersion = json.getString("tag_name").replace("v", "")
                val currentVersion = BuildConfig.VERSION_NAME.replace("v", "")

                lastCheckTime = System.currentTimeMillis()

                if (isNewerVersion(currentVersion, latestVersion)) {
                    val assets = json.optJSONArray("assets")
                    var apkUrl = ""

                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) {
                                apkUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                    }

                    if (apkUrl.isEmpty()) {
                        apkUrl = json.getString("html_url")
                    }

                    val info = AppUpdateInfo(
                        version = json.getString("tag_name"),
                        releaseNotes = json.getString("body"),
                        downloadUrl = apkUrl
                    )
                    cachedUpdateInfo = info
                    _updateInfo.value = info
                } else {
                    cachedUpdateInfo = null
                    _updateInfo.value = null
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to check for updates", e)
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.substringBefore("-")
        val cleanLatest = latest.substringBefore("-")

        val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = cleanLatest.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }

        val currentIsPreRelease = current.contains("-")
        val latestIsPreRelease = latest.contains("-")
        if (currentIsPreRelease && !latestIsPreRelease) return true

        return false
    }

    fun triggerUpdateDownload(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
package com.zenx.yugen.play.data.repository

import android.util.Log
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.data.model.ReleaseNotes
import com.zenx.yugen.play.util.ReleaseNotesParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseNotesRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val playerPreferences: PlayerPreferences
) {
    private val tag = "ReleaseNotesRepo"

    fun getReleaseNotes(targetVersion: String = BuildConfig.VERSION_NAME): Flow<ReleaseNotes> = flow {
        var hasEmitted = false

        // 1. Check local DataStore cache first
        try {
            val cachedVersion = playerPreferences.cachedReleaseNotesVersion.first()
            val cachedBody = playerPreferences.cachedReleaseNotesBody.first()

            if (cachedVersion == targetVersion && cachedBody.isNotBlank()) {
                val parsed = ReleaseNotesParser.parse(cachedBody, targetVersion)
                emit(parsed)
                hasEmitted = true
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to read cached release notes", e)
        }

        // 2. Fetch fresh release notes from GitHub Releases API
        try {
            val cleanVersion = targetVersion.removePrefix("v")
            val releaseBody = fetchReleaseBodyFromGithub(cleanVersion)

            if (!releaseBody.isNullOrBlank()) {
                playerPreferences.setCachedReleaseNotes(targetVersion, releaseBody)
                val parsed = ReleaseNotesParser.parse(releaseBody, targetVersion)
                emit(parsed)
                hasEmitted = true
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch remote release notes for v$targetVersion", e)
        }

        // 3. If nothing was emitted (offline on fresh install or API failed), use bundled fallback
        if (!hasEmitted) {
            emit(ReleaseNotesParser.fallbackReleaseNotes(targetVersion))
        }
    }.flowOn(Dispatchers.IO)

    private fun fetchReleaseBodyFromGithub(version: String): String? {
        val urlsToTry = listOf(
            "https://api.github.com/repos/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project/releases/tags/v$version",
            "https://api.github.com/repos/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project/releases/tags/$version",
            "https://api.github.com/repos/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project/releases/latest"
        )

        for (url in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "YugenPlay-App")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseText = response.body.string()
                        val json = JSONObject(responseText)
                        val body = json.optString("body")
                        if (body.isNotBlank()) {
                            return body
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(tag, "Attempt failed for url $url: ${e.message}")
            }
        }
        return null
    }
}

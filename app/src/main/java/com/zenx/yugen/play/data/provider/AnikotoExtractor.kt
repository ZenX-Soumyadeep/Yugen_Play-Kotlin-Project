package com.zenx.yugen.play.data.provider

import android.util.Log
import com.zenx.yugen.play.domain.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request

class AnikotoExtractor(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String
) {

    companion object {
        private const val TAG = "AnikotoExtractor"
    }

    fun isMegaPlayServer(serverName: String): Boolean {
        val name = serverName.lowercase().replace(" ", "").replace("-", "")
        return name in setOf("vidstream2", "hd1", "hd2", "vidcloud1", "vidplay1", "kiwistream") ||
                name.contains("vidstream") || name.contains("hd1") || name.contains("hd2")
    }

    fun isMegaPlayUrl(url: String): Boolean {
        return url.contains("megaplay", ignoreCase = true)
    }

    suspend fun getEmbedLink(serverId: String, epUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val listHeaders = Headers.Builder().apply {
                add("Accept", "application/json, text/javascript, */*; q=0.01")
                add("Referer", "$baseUrl$epUrl")
                add("X-Requested-With", "XMLHttpRequest")
            }.build()

            val request = Request.Builder()
                .url("$baseUrl/ajax/server?get=$serverId")
                .headers(listHeaders)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bodyStr = response.body?.string() ?: return@use null

                val jsonObj = json.parseToJsonElement(bodyStr).jsonObject
                val embedUrl = jsonObj["result"]?.jsonObject?.get("url")?.jsonPrimitive?.content

                embedUrl?.replace("\\/", "/")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get embed link for server $serverId: ${e.message}")
            null
        }
    }

    private val megaPlayExtractor = com.zenx.yugen.play.data.extractor.MegaPlayExtractor(client, json)

    suspend fun extractFromMegaPlay(
        embedUrl: String,
        formattedServerName: String,
        qualityTag: String
    ): List<VideoStream> {
        return megaPlayExtractor.extract(
            embedUrl = embedUrl,
            referer = "$baseUrl/",
            serverName = formattedServerName,
            qualityTag = qualityTag
        )
    }
}
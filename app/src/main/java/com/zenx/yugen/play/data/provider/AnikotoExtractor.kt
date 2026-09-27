package com.zenx.yugen.play.data.provider

import android.util.Base64
import android.util.Log
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.domain.Subtitle
import com.zenx.yugen.play.domain.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AnikotoExtractor(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String
) {

    companion object {
        private const val TAG = "AnikotoExtractor"
        private const val MEGAPLAY_AES_KEY = "i?LMTAx0Q6,:}50U"
        private const val MEGAPLAY_AES_IV = "W0;27ToaUpl_P%'c"

        private val DATA_ID_REGEX = Regex("""data-id="([^"]+)"""")
        private val FILE_ID_REGEX = Regex("""[?&]id=([^&"']+)""")
        private val FILE_JSON_REGEX = Regex(""""file"\s*:\s*"([^"]+)"""")
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

    suspend fun extractFromMegaPlay(
        embedUrl: String,
        formattedServerName: String,
        qualityTag: String
    ): List<VideoStream> = withContext(Dispatchers.IO) {
        try {
            val pageHeaders = Headers.Builder()
                .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Referer", "$baseUrl/")
                .build()

            val request = Request.Builder()
                .url(embedUrl)
                .headers(pageHeaders)
                .get()
                .build()

            val pageBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("MegaPlay page failed: HTTP ${response.code}")
                response.body?.string() ?: throw Exception("Empty body")
            }

            val mediaId = parseMegaPlayMediaId(pageBody)
                ?: throw Exception("Failed to find MegaPlay media ID")

            val getSourcesUrl = buildMegaPlayGetSourcesUrl(embedUrl, mediaId)

            val apiHeaders = Headers.Builder().apply {
                add("Accept", "application/json,*/*")
                add("X-Requested-With", "XMLHttpRequest")
                add("Referer", embedUrl)
            }.build()

            val sourcesRequest = Request.Builder()
                .url(getSourcesUrl)
                .headers(apiHeaders)
                .get()
                .build()

            val sourcesBody = client.newCall(sourcesRequest).execute().use { response ->
                if (!response.isSuccessful) throw Exception("MegaPlay getSources failed: HTTP ${response.code}")
                response.body?.string() ?: throw Exception("Empty body")
            }

            val sourcesJson = json.parseToJsonElement(sourcesBody).jsonObject
            val encData = sourcesJson["enc"]?.jsonPrimitive?.content
            val rawSource = sourcesJson["sources"]?.jsonPrimitive?.content

            val m3u8 = processMegaPlaySource(encData, rawSource)
                ?: throw Exception("Failed to decrypt/find MegaPlay source")

            val subtitles = mutableListOf<Subtitle>()
            sourcesJson["tracks"]?.let { tracksElement ->
                if (tracksElement is JsonArray) {
                    tracksElement.forEach { trackObj ->
                        val track = trackObj.jsonObject
                        val file = track["file"]?.jsonPrimitive?.content ?: ""
                        val label = track["label"]?.jsonPrimitive?.content ?: ""
                        if (label.isNotBlank() && file.isNotBlank()) {
                            subtitles.add(Subtitle(
                                label = label,
                                url = file,
                                format = if (file.endsWith(".ass")) "ASS" else "VTT"
                            ))
                        }
                    }
                }
            }

            val skipIntervals = mutableListOf<SkipInterval>()
            sourcesJson["intro"]?.jsonObject?.let { intro ->
                val start = intro["start"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val end = intro["end"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                if (start != 0.0 || end != 0.0) skipIntervals.add(SkipInterval(start, end, "Intro"))
            }
            sourcesJson["outro"]?.jsonObject?.let { outro ->
                val start = outro["start"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val end = outro["end"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                if (start != 0.0 || end != 0.0) skipIntervals.add(SkipInterval(start, end, "Outro"))
            }

            val host = embedUrl.toHttpUrlOrNull()?.host ?: "megaplay.buzz"
            val vidHeaders = mapOf(
                "Referer" to "https://$host/",
                "Origin" to "https://$host",
                "Accept" to "*/*"
            )

            listOf(
                VideoStream(
                    quality = qualityTag,
                    url = m3u8,
                    headers = vidHeaders,
                    isM3U8 = true,
                    subtitles = subtitles,
                    skipIntervals = skipIntervals,
                    serverName = formattedServerName,
                    format = "HLS"
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "extractFromMegaPlay failed: ${e.message}")
            emptyList()
        }
    }

    private fun parseMegaPlayMediaId(html: String): String? {
        val dataId = DATA_ID_REGEX.find(html)?.groupValues?.get(1)?.trim()
        if (!dataId.isNullOrBlank()) return dataId
        return FILE_ID_REGEX.find(html)?.groupValues?.get(1)
    }

    private fun buildMegaPlayGetSourcesUrl(embedUrl: String, id: String): String {
        val base = embedUrl.toHttpUrlOrNull()?.let {
            "${it.scheme}://${it.host}/stream/getSources"
        } ?: "https://megaplay.buzz/stream/getSources"

        val urlBuilder = base.toHttpUrlOrNull()?.newBuilder()?.addQueryParameter("id", id)
            ?: return "$base?id=$id"

        embedUrl.toHttpUrlOrNull()?.queryParameter("s")?.let {
            urlBuilder.addQueryParameter("s", it)
        }

        return urlBuilder.build().toString()
    }

    private fun processMegaPlaySource(enc: String?, source: String?): String? {
        var m3u8: String? = null

        if (!enc.isNullOrBlank() && enc != "null") {
            try {
                val keyBytes = ByteArray(32)
                val keySrc = MEGAPLAY_AES_KEY.toByteArray(StandardCharsets.UTF_8)
                System.arraycopy(keySrc, 0, keyBytes, 0, keySrc.size.coerceAtMost(32))

                val iv = MEGAPLAY_AES_IV.toByteArray(StandardCharsets.UTF_8)

                val encrypted = Base64.decode(
                    enc.replace('-', '+').replace('_', '/'),
                    Base64.DEFAULT
                )

                if (encrypted.isNotEmpty() && encrypted.size % 16 == 0) {
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        SecretKeySpec(keyBytes, "AES"),
                        IvParameterSpec(iv)
                    )
                    val decrypted = cipher.doFinal(encrypted)
                    val jsonStr = String(decrypted, StandardCharsets.UTF_8)

                    val fileMatch = FILE_JSON_REGEX.find(jsonStr)
                    if (fileMatch != null) {
                        m3u8 = fileMatch.groupValues[1].replace("\\/", "/")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MegaPlay AES decrypt failed: ${e.message}")
            }
        }

        if (m3u8.isNullOrBlank() || m3u8 == "null") {
            m3u8 = source
        }

        return m3u8
    }
}
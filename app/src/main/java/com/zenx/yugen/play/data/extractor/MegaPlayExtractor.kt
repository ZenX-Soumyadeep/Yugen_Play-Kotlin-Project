package com.zenx.yugen.play.data.extractor

import android.util.Log
import com.zenx.yugen.play.data.crypto.AesCipher
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.domain.Subtitle
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.extractor.EmbedExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MegaPlayExtractor @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) : EmbedExtractor {

    override val name: String = "MegaPlay"
    override val hostPattern: Regex = Regex("megaplay|vidstream|vidcloud|vidplay|kiwistream", RegexOption.IGNORE_CASE)

    companion object {
        private const val TAG = "MegaPlayExtractor"
        private const val MEGAPLAY_AES_KEY = "i?LMTAx0Q6,:}50U"
        private const val MEGAPLAY_AES_IV = "W0;27ToaUpl_P%'c"

        private val DATA_ID_REGEX = Regex("""data-id="([^"]+)"""")
        private val FILE_ID_REGEX = Regex("""[?&]id=([^&"']+)""")
        private val FILE_JSON_REGEX = Regex(""""file"\s*:\s*"([^"]+)"""")
    }

    override suspend fun extract(
        embedUrl: String,
        referer: String,
        serverName: String?,
        qualityTag: String
    ): List<VideoStream> = withContext(Dispatchers.IO) {
        try {
            val pageHeaders = Headers.Builder()
                .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .add("X-Requested-With", "XMLHttpRequest")
                .apply {
                    if (referer.isNotBlank()) add("Referer", referer)
                }
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
                            subtitles.add(
                                Subtitle(
                                    label = label,
                                    url = file,
                                    format = if (file.endsWith(".ass")) "ASS" else "VTT"
                                )
                            )
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
                    serverName = serverName,
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
                val jsonStr = AesCipher.decryptCbc(enc, MEGAPLAY_AES_KEY, MEGAPLAY_AES_IV)
                if (jsonStr != null) {
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

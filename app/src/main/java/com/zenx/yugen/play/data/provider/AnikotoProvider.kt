package com.zenx.yugen.play.data.provider

import android.util.Base64
import android.util.Log
import com.zenx.yugen.play.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AnikotoProvider(
    private val client: OkHttpClient,
    private val json: Json
) : AnimeProvider {

    override val name: String = "Anikoto"
    override val baseUrl: String = "https://anikototv.to"

    private val extractor = AnikotoExtractor(client, json, baseUrl)
    private val mapperUrl = "https://mapper.nekostream.site/api"

    companion object {
        private const val TAG = "AnikotoProvider"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

        private val EXCHANGE_KEY_1 = listOf("AP6GeR8H0lwUz1", "UAz8Gwl10P6ReH")
        private const val KEY_1 = "ItFKjuWokn4ZpB"
        private const val KEY_2 = "fOyt97QWFB3"
        private val EXCHANGE_KEY_2 = listOf("1majSlPQd2M5", "da1l2jSmP5QM")
        private val EXCHANGE_KEY_3 = listOf("CPYvHj09Au3", "0jHA9CPYu3v")
        private const val KEY_3 = "736y1uTJpBLUX"

        private val VRF_ORDER = listOf(
            Triple(1, "exchange", EXCHANGE_KEY_1),
            Triple(2, "rc4", listOf(KEY_1)),
            Triple(3, "rc4", listOf(KEY_2)),
            Triple(4, "exchange", EXCHANGE_KEY_2),
            Triple(5, "exchange", EXCHANGE_KEY_3),
            Triple(6, "reverse", emptyList()),
            Triple(7, "rc4", listOf(KEY_3)),
            Triple(8, "base64", emptyList()),
        )
    }

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
            val vrf = if (query.isNotEmpty()) vrfEncrypt(query) else ""
            val url = "$baseUrl/search?keyword=$encodedQuery&vrf=$vrf"

            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "$baseUrl/")
                .addHeader("User-Agent", USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList<SearchResult>()
                val html = response.body?.string() ?: return@use emptyList<SearchResult>()
                val doc = Jsoup.parse(html)

                doc.select("div.ani.items > div.item").mapNotNull { element ->
                    val aName = element.selectFirst("a.name") ?: return@mapNotNull null
                    val title = aName.text()
                    val href = aName.attr("href").substringBefore("?").trim()
                    val urlPath = if (href.startsWith(baseUrl)) href.substring(baseUrl.length) else href

                    val poster = element.selectFirst("img")?.let {
                        it.attr("data-src").ifBlank { it.attr("src") }
                    }.orEmpty()

                    if (title.isBlank() || urlPath.isBlank()) null
                    else SearchResult(title = title, url = urlPath, poster = poster)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getEpisodes(animeUrl: String): List<Episode> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(baseUrl + animeUrl)
                .addHeader("Referer", "$baseUrl/")
                .addHeader("User-Agent", USER_AGENT)
                .get()
                .build()

            var dataId = ""
            var malId = ""
            var slug = ""
            var title = ""

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList<Episode>()
                val html = response.body?.string() ?: return@use emptyList<Episode>()
                val doc = Jsoup.parse(html)

                title = doc.selectFirst(".name")?.text().orEmpty()
                dataId = doc.selectFirst("[data-id]")?.attr("data-id")
                    ?: doc.selectFirst("[data-tip]")?.attr("data-tip")
                            ?: return@use emptyList<Episode>()

                val watchDiv = doc.selectFirst("#watch-main")
                if (watchDiv != null) {
                    malId = watchDiv.attr("data-mal").orEmpty()
                }
            }

            if (dataId.isBlank()) return@withContext emptyList<Episode>()

            val listHeaders = Headers.Builder().apply {
                add("Accept", "application/json, text/javascript, */*; q=0.01")
                add("Referer", baseUrl + animeUrl)
                add("User-Agent", USER_AGENT)
                add("X-Requested-With", "XMLHttpRequest")
            }.build()

            val epsRequest = Request.Builder()
                .url("$baseUrl/ajax/episode/list/$dataId?vrf=${vrfEncrypt(dataId)}")
                .headers(listHeaders)
                .get()
                .build()

            client.newCall(epsRequest).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList<Episode>()
                val jsonStr = response.body?.string() ?: return@use emptyList<Episode>()

                var htmlContent: String? = null
                try {
                    val jsonObj = json.parseToJsonElement(jsonStr).jsonObject
                    htmlContent = jsonObj["html"]?.jsonPrimitive?.content ?: jsonObj["result"]?.jsonPrimitive?.content
                } catch(e: Exception) {
                    val regex = Regex(""""(?:result|html)"\s*:\s*"([^"]+)"""")
                    htmlContent = regex.find(jsonStr)?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\/", "/")?.replace("\\n", "")
                }

                if (htmlContent == null) return@use emptyList<Episode>()

                val epsDoc = Jsoup.parse(htmlContent)

                epsDoc.select("div.episodes ul > li > a").mapNotNull { element ->
                    val epNum = element.attr("data-num")
                    val ids = element.attr("data-ids")

                    val timestamp = element.attr("data-timestamp").toLongOrNull() ?: 0L
                    val elemMalId = element.attr("data-mal").takeIf { it.isNotEmpty() } ?: malId
                    val elemSlug = element.attr("data-slug").takeIf { it.isNotEmpty() } ?: slug

                    if (epNum.isBlank() || ids.isBlank()) return@mapNotNull null

                    val stateJson = buildJsonObject {
                        put("ids", ids)
                        put("epurl", "$animeUrl/ep-$epNum")
                        put("mal", elemMalId)
                        put("slug", elemSlug)
                        put("ts", timestamp)
                    }
                    val safeSourceUrl = Base64.encodeToString(stateJson.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)

                    val epName = element.parent()?.select("span.d-title")?.text().orEmpty()
                    val displayTitle = if (epName.isNotEmpty() && epName != "Episode $epNum") "Episode $epNum: $epName" else "Episode $epNum"

                    val generatedId = EpisodeId.build(
                        sourceUrl = safeSourceUrl,
                        animeTitle = title,
                        episodeNumber = epNum.toFloatOrNull() ?: 1f
                    )

                    Episode(
                        id = generatedId,
                        number = epNum.toFloatOrNull() ?: 1f,
                        title = displayTitle,
                        thumbnail = null
                    )
                }.reversed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getEpisodes failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun extractStreams(episodeId: String, title: String): List<VideoStream> = withContext(Dispatchers.IO) {
        val parsedId = EpisodeId.parse(episodeId)

        var ids = ""
        var epurlPart = ""
        var malId = ""
        var slug = ""
        var ts = ""

        try {
            val decodedJson = String(Base64.decode(parsedId.sourceUrl, Base64.URL_SAFE or Base64.NO_WRAP))
            val stateObj = json.parseToJsonElement(decodedJson).jsonObject
            ids = stateObj["ids"]?.jsonPrimitive?.content ?: ""
            epurlPart = stateObj["epurl"]?.jsonPrimitive?.content ?: ""
            malId = stateObj["mal"]?.jsonPrimitive?.content ?: ""
            slug = stateObj["slug"]?.jsonPrimitive?.content ?: ""
            ts = stateObj["ts"]?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode state JSON: ${e.message}")
            return@withContext emptyList()
        }

        val servers = mutableListOf<VideoServer>()

        // 1. Fetch HTML Servers (SUB/DUB)
        try {
            val listHeaders = Headers.Builder().apply {
                add("Accept", "application/json, text/javascript, */*; q=0.01")
                add("Referer", "$baseUrl$epurlPart")
                add("User-Agent", USER_AGENT)
                add("X-Requested-With", "XMLHttpRequest")
            }.build()

            val req = Request.Builder()
                .url("$baseUrl/ajax/server/list?servers=$ids")
                .headers(listHeaders)
                .get()
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: ""
                    val jsonObj = json.parseToJsonElement(jsonStr).jsonObject
                    val htmlContent = jsonObj["html"]?.jsonPrimitive?.content ?: jsonObj["result"]?.jsonPrimitive?.content ?: ""
                    val doc = Jsoup.parse(htmlContent)

                    val serverItems = doc.select(".server-item")
                    if (serverItems.isNotEmpty()) {
                        serverItems.forEach { item ->
                            val serverType = item.attr("data-type").ifBlank {
                                val pType = item.parent()?.attr("data-type") ?: ""
                                if (item.closest(".servers-dub") != null || pType.contains("dub", true)) {
                                    if (pType.contains("hdub", true)) "hdub" else "dub"
                                } else if (item.closest(".servers-hsub") != null || pType.contains("hsub", true)) {
                                    "hsub"
                                } else {
                                    "sub"
                                }
                            }
                            val typeLabel = when {
                                serverType.equals("hdub", true) -> "HDUB"
                                serverType.equals("hsub", true) -> "HSUB"
                                serverType.equals("dub", true) -> "DUB"
                                else -> "SUB"
                            }
                            val serverId = item.attr("data-link-id").ifBlank { item.attr("data-id") }
                            val serverName = item.text().trim()
                            if (serverId.isNotBlank() && serverName.isNotBlank()) {
                                servers.add(VideoServer(typeLabel, serverId, serverName))
                            }
                        }
                    } else {
                        doc.select(".servers .type").forEach { typeElement ->
                            val serverType = typeElement.attr("data-type")
                            val typeLabel = when {
                                serverType.equals("hdub", true) -> "HDUB"
                                serverType.equals("hsub", true) -> "HSUB"
                                serverType.equals("dub", true) -> "DUB"
                                else -> "SUB"
                            }

                            typeElement.select("li").forEach { li ->
                                val serverId = li.attr("data-link-id").ifBlank { li.attr("data-id") }
                                val serverName = li.text().trim()
                                if (serverId.isNotBlank() && serverName.isNotBlank()) {
                                    servers.add(VideoServer(typeLabel, serverId, serverName))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch HTML servers: ${e.message}")
        }

        // 2. Fetch Mapper Servers (HSUB/DUB)
        if (malId.isNotBlank() && slug.isNotBlank() && ts.isNotBlank() && ts != "0") {
            try {
                val mapperHeaders = Headers.Builder().apply {
                    add("Accept", "application/json, text/javascript, */*; q=0.01")
                    add("Referer", "$baseUrl/")
                    add("Origin", baseUrl)
                }.build()

                val apiUrl = "$mapperUrl/mal/$malId/$slug/$ts"
                val req = Request.Builder().url(apiUrl).headers(mapperHeaders).addHeader("User-Agent", USER_AGENT).get().build()

                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonStr = response.body?.string() ?: ""
                        val mapperObj = json.parseToJsonElement(jsonStr).jsonObject

                        mapperObj.keys.filter { !it.equals("status", true) }.forEach { key ->
                            val serverObj = mapperObj[key]?.jsonObject
                            val serverName = getMapperServerName(key)

                            listOf("sub" to "HSUB", "dub" to "DUB").forEach { (typeKey, typeLabel) ->
                                val linkUrl = serverObj?.get(typeKey)?.jsonObject?.get("url")?.jsonPrimitive?.content
                                if (!linkUrl.isNullOrBlank() && linkUrl != "null") {
                                    servers.add(VideoServer(typeLabel, linkUrl, serverName))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch Mapper servers: ${e.message}")
            }
        }

        // 3. Extract Streams Concurrently
        coroutineScope {
            servers.distinctBy { "${it.type}_${it.serverId}" }.map { server ->
                async {
                    try {
                        val formattedName = server.serverName
                        val qualityTag = "1080p [${server.type}]"

                        if (extractor.isMegaPlayServer(server.serverName) || extractor.isMegaPlayUrl(server.serverId)) {
                            val embedUrl = if (server.serverId.startsWith("http")) server.serverId
                            else extractor.getEmbedLink(server.serverId, epurlPart)

                            if (embedUrl != null) {
                                extractor.extractFromMegaPlay(embedUrl, formattedName, qualityTag)
                            } else emptyList()
                        } else emptyList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Extraction failed for server ${server.serverName}: ${e.message}")
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }

    private data class VideoServer(val type: String, val serverId: String, val serverName: String)

    private fun getMapperServerName(key: String): String {
        return when (key.lowercase()) {
            "vcloud" -> "VidCloud-1"
            "vplay" -> "VidPlay-1"
            "vstream" -> "Vidstream-2"
            "mew" -> "Mew"
            else -> key
        }
    }

    private fun vrfEncrypt(input: String): String {
        var vrf = input
        VRF_ORDER.forEach { item ->
            when (item.second) {
                "exchange" -> vrf = exchange(vrf, item.third)
                "rc4" -> vrf = rc4Encrypt(item.third[0], vrf)
                "reverse" -> vrf = vrf.reversed()
                "base64" -> vrf = Base64.encode(vrf.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP).toString(Charsets.UTF_8).trimEnd('=')
            }
        }
        return URLEncoder.encode(vrf, "utf-8")
    }

    private fun rc4Encrypt(key: String, input: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.ENCRYPT_MODE, rc4Key, cipher.parameters)
        val output = cipher.doFinal(input.toByteArray())
        return Base64.encode(output, Base64.URL_SAFE or Base64.NO_WRAP).toString(Charsets.UTF_8).trimEnd('=')
    }

    private fun exchange(input: String, keys: List<String>): String {
        val key1 = keys[0]
        val key2 = keys[1]
        return input.map { i ->
            val idx = key1.indexOf(i)
            if (idx != -1) key2[idx] else i
        }.joinToString("")
    }
}
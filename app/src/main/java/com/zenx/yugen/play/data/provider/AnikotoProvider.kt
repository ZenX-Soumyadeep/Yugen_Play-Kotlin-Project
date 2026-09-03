package com.zenx.yugen.play.data.provider

import android.util.Base64
import android.util.Log
import com.zenx.yugen.play.domain.AnimeProvider
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.SearchResult
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.domain.Subtitle
import com.zenx.yugen.play.domain.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(e)
            }
        }
    })
}

class AnikotoProvider(
    private val client: OkHttpClient
) : AnimeProvider {

    override val name: String = "Anikoto"
    override val baseUrl: String = "https://anikoto.cz"

    private val tag = "YUGEN_PLAYER"

    // --- VRF Cryptography Engine ---
    private val exchangeKey1 = listOf("AP6GeR8H0lwUz1", "UAz8Gwl10P6ReH")
    private val key1 = "ItFKjuWokn4ZpB"
    private val key2 = "fOyt97QWFB3"
    private val exchangeKey2 = listOf("1majSlPQd2M5", "da1l2jSmP5QM")
    private val exchangeKey3 = listOf("CPYvHj09Au3", "0jHA9CPYu3v")
    private val key3 = "736y1uTJpBLUX"

    private fun vrfEncrypt(input: String): String {
        var vrf = input
        vrf = exchange(vrf, exchangeKey1)
        vrf = rc4Encrypt(key1, vrf)
        vrf = rc4Encrypt(key2, vrf)
        vrf = exchange(vrf, exchangeKey2)
        vrf = exchange(vrf, exchangeKey3)
        vrf = vrf.reversed()
        vrf = rc4Encrypt(key3, vrf)
        vrf = Base64.encodeToString(vrf.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        return URLEncoder.encode(vrf, "utf-8")
    }

    private fun rc4Encrypt(key: String, input: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.ENCRYPT_MODE, rc4Key)
        val output = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(output, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun exchange(input: String, keys: List<String>): String {
        val sourceChars = keys[0]
        val targetChars = keys[1]
        return input.map { i ->
            val idx = sourceChars.indexOf(i)
            if (idx != -1) targetChars[idx] else i
        }.joinToString("")
    }

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val vrf = if (query.isNotEmpty()) vrfEncrypt(query) else ""
        val searchUrl = "$baseUrl/filter?keyword=$encodedQuery&vrf=$vrf"

        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", "$baseUrl/")
            .build()

        val html = try {
            client.newCall(request).await().use { response ->
                response.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            Log.e(tag, "Search request failed", e)
            return@withContext emptyList()
        }

        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()
        val items = doc.select(".flw-item, div.item, .film_list-wrap > div, .film-detail, div.ani.items > div.item")

        items.forEach { item ->
            val aTag = item.selectFirst("a.name, a.poster, a.film-poster, a.dynamic-name, a[href*='/watch/']")
            val imgTag = item.selectFirst("img")
            val titleTag = item.selectFirst(".name, .film-name, h3.title, .film-name a, a.name")

            if (aTag != null && (titleTag != null || aTag.hasAttr("title"))) {
                val rawHref = aTag.attr("href").substringBefore("?")
                val cleanHref = rawHref.replace(Regex("""/ep-\d+$"""), "")
                val url = if (cleanHref.startsWith("http")) cleanHref else "$baseUrl$cleanHref"
                val title = titleTag?.text()?.trim() ?: aTag.attr("title").trim()
                val poster = imgTag?.attr("data-src")?.ifEmpty { imgTag.attr("src") }
                    ?: imgTag?.attr("src").orEmpty()

                if (title.isNotEmpty()) {
                    results.add(SearchResult(title = title, url = url, poster = poster))
                }
            }
        }
        results
    }

    override suspend fun getEpisodes(animeUrl: String): List<Episode> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(animeUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", "$baseUrl/")
            .build()

        val html = try {
            client.newCall(request).await().use { response ->
                response.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed fetching anime page: $animeUrl", e)
            return@withContext emptyList()
        }

        val doc = Jsoup.parse(html)
        val episodes = mutableListOf<Episode>()

        val animeId = doc.selectFirst("[data-id]")?.attr("data-id")
            ?: doc.selectFirst("[data-tip]")?.attr("data-tip")
            ?: ""

        if (animeId.isNotEmpty()) {
            val vrf = vrfEncrypt(animeId)
            val ajaxUrl = "$baseUrl/ajax/episode/list/$animeId?vrf=$vrf"

            try {
                val ajaxReq = Request.Builder()
                    .url(ajaxUrl)
                    .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                    .addHeader("Referer", animeUrl)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val ajaxResp = client.newCall(ajaxReq).await().use { it.body?.string().orEmpty() }

                if (ajaxResp.isNotEmpty()) {
                    val ajaxHtml = if (ajaxResp.startsWith("{")) {
                        val json = JSONObject(ajaxResp)
                        json.optString("result", json.optString("html", ""))
                    } else {
                        ajaxResp
                    }

                    if (ajaxHtml.isNotEmpty()) {
                        val ajaxDoc = Jsoup.parse(ajaxHtml)
                        val epElements = ajaxDoc.select("div.episodes ul > li > a, a.ep-item, a[data-id], .ssl-item a, li a")

                        epElements.forEach { epElement ->
                            val epNum = epElement.attr("data-num")
                            val ids = epElement.attr("data-ids").ifEmpty { epElement.attr("data-id") }
                            val tooltip = epElement.parent()?.attr("title").orEmpty()

                            var title = epElement.parent()?.select("span.d-title")?.text().orEmpty()
                            if (title.isEmpty() && tooltip.isNotEmpty()) {
                                title = tooltip.substringBefore("Release:").substringBefore("Softsub").trim()
                            }
                            if (title.isEmpty()) title = "Episode $epNum"

                            val compoundId = "$animeUrl~~~$ids~~~$epNum"
                            episodes.add(Episode(id = compoundId, title = title, number = epNum.toFloatOrNull() ?: 0f))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "AJAX episode fetch failed for: $ajaxUrl", e)
            }
        }

        if (episodes.isEmpty()) {
            episodes.add(Episode(id = "$animeUrl~~~$animeId~~~1", title = "Full Movie / Episode 1", number = 1f))
        }

        episodes.sortedBy { it.number }
    }

    override suspend fun extractStreams(episodeId: String, title: String): List<VideoStream> = withContext(Dispatchers.IO) {
        val parts = episodeId.split("~~~")
        val epUrl = parts[0]
        val serverParam = if (parts.size > 1) parts[1] else ""

        val serverUrl = "$baseUrl/ajax/server/list?servers=$serverParam"
        var serverHtml = ""

        try {
            val serverReq = Request.Builder()
                .url(serverUrl)
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Referer", epUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val respStr = client.newCall(serverReq).await().use { it.body?.string().orEmpty() }

            if (respStr.trim().startsWith("{")) {
                val json = JSONObject(respStr)
                serverHtml = json.optString("result", "")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed endpoint: $serverUrl", e)
        }

        val serverDoc = Jsoup.parse(serverHtml)
        val serverElements = serverDoc.select("div.servers > div.type li")
            .filter { !it.hasClass("download-icon") && !it.hasClass("nav-item") && !it.hasClass("tab-item") }

        val semaphore = Semaphore(3)

        val deferredStreams = serverElements.map { serverElement ->
            async {
                semaphore.withPermit {
                    val serverId = serverElement.attr("data-link-id")
                    val serverName = serverElement.text().trim().ifEmpty { "Server" }
                    val typeElem = serverElement.closest(".type")
                    val typeStr = typeElem?.selectFirst("label")?.text().orEmpty()
                        .ifEmpty { typeElem?.attr("data-type").orEmpty() }

                    val prefix = if (typeStr.contains("dub", true)) "[DUB]" else "[SUB]"
                    val extractedList = mutableListOf<VideoStream>()

                    if (serverId.isNotEmpty()) {
                        try {
                            val embedReq = Request.Builder()
                                .url("$baseUrl/ajax/server?get=$serverId")
                                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                                .addHeader("Referer", epUrl)
                                .addHeader("X-Requested-With", "XMLHttpRequest")
                                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                .build()

                            val embedResp = client.newCall(embedReq).await().use { it.body?.string().orEmpty() }

                            if (embedResp.trim().startsWith("{")) {
                                val json = JSONObject(embedResp)
                                val resultObj = json.optJSONObject("result")
                                val rawEmbedUrl = resultObj?.optString("url", "")
                                    ?: json.optString("url", "")

                                val skipIntervals = mutableListOf<SkipInterval>()
                                val skipDataObj = resultObj?.optJSONObject("skip_data")
                                if (skipDataObj != null) {
                                    val introArr = skipDataObj.optJSONArray("intro")
                                    if (introArr != null && introArr.length() >= 2) {
                                        val start = introArr.optDouble(0)
                                        val end = introArr.optDouble(1)
                                        if (end > start) skipIntervals.add(SkipInterval(start, end, "op"))
                                    }
                                    val outroArr = skipDataObj.optJSONArray("outro")
                                    if (outroArr != null && outroArr.length() >= 2) {
                                        val start = outroArr.optDouble(0)
                                        val end = outroArr.optDouble(1)
                                        if (end > start) skipIntervals.add(SkipInterval(start, end, "ed"))
                                    }
                                }

                                if (rawEmbedUrl.isNotEmpty()) {
                                    val embedUrl = normalizeUrl(rawEmbedUrl, baseUrl)
                                    val resolved = resolveEmbedStreams(embedUrl, epUrl, serverName, prefix, skipIntervals)
                                    if (resolved.isNotEmpty()) {
                                        extractedList.addAll(resolved)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Server extraction failed for $serverId", e)
                        }
                    }
                    extractedList
                }
            }
        }

        deferredStreams.awaitAll().flatten()
    }

    private suspend fun resolveEmbedStreams(
        embedUrl: String,
        referer: String,
        serverName: String,
        prefix: String,
        skipIntervals: List<SkipInterval>
    ): List<VideoStream> {
        val resultStreams = mutableListOf<VideoStream>()
        try {
            if (embedUrl.contains("mewcdn.online/player/plyr.php")) {
                val fragment = embedUrl.substringAfter("#").substringBefore("#")
                if (fragment.isNotEmpty()) {
                    val rawM3u8 = String(Base64.decode(fragment, Base64.DEFAULT), Charsets.UTF_8).trim()

                    val pageHeaders = mapOf(
                        "Referer" to "$baseUrl/",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    )

                    val reqBuilder = Request.Builder().url(embedUrl)
                    pageHeaders.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

                    val pageHtml = client.newCall(reqBuilder.build()).await().use { it.body?.string().orEmpty() }
                    val hostMapRegex = Regex("""var HOST_MAP\s*=\s*\{([^}]+)\}""")
                    val entryRegex = Regex("""'([^']+)'\s*:\s*'([^']+)'""")

                    val mapMatch = hostMapRegex.find(pageHtml)
                    val hostMap = mutableMapOf<String, String>()
                    if (mapMatch != null) {
                        entryRegex.findAll(mapMatch.groupValues[1]).forEach {
                            hostMap[it.groupValues[1]] = it.groupValues[2]
                        }
                    }

                    var finalM3u8 = rawM3u8
                    for ((origin, proxy) in hostMap) {
                        if (finalM3u8.contains(origin)) {
                            finalM3u8 = finalM3u8.replace(origin, proxy)
                            break
                        }
                    }

                    val mewHeaders = mapOf(
                        "Referer" to "https://mewcdn.online/",
                        "Origin" to "https://mewcdn.online",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    )

                    val parsedVariants = parseM3U8(finalM3u8, mewHeaders, serverName, prefix, emptyList(), skipIntervals)
                    if (parsedVariants.isNotEmpty()) {
                        resultStreams.addAll(parsedVariants)
                    } else {
                        resultStreams.add(
                            VideoStream(
                                quality = "$prefix $serverName",
                                url = finalM3u8,
                                headers = mewHeaders,
                                isM3U8 = true,
                                subtitles = emptyList(),
                                skipIntervals = skipIntervals,
                                format = "HLS",
                                resolution = "Auto",
                                serverName = "$prefix $serverName"
                            )
                        )
                    }
                    return resultStreams
                }
            }

            val uri = URI(embedUrl)
            val host = uri.host ?: "anikoto.cz"
            val baseOrigin = "https://$host"

            val pageBody = client.newCall(
                Request.Builder()
                    .url(embedUrl)
                    .addHeader("Referer", referer)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            ).await().use { it.body?.string().orEmpty() }

            val dataId = Regex("""data-id="([^"]+)"""").find(pageBody)?.groupValues?.get(1).orEmpty()

            if (dataId.isNotEmpty()) {
                val streamType = try {
                    uri.path.split("/").filter { it.isNotEmpty() }
                        .lastOrNull()?.takeIf { it == "sub" || it == "dub" || it == "hsub" } ?: ""
                } catch (_: Exception) { "" }

                val candidateApiUrls = listOf(
                    "$baseOrigin/stream/getSources?id=$dataId&id=$dataId&type=$streamType&type=$streamType",
                    "$baseOrigin/stream/getSourcesNew?id=$dataId&id=$dataId&type=$streamType&type=$streamType"
                )

                var finalStreamUrl = ""
                val subtitles = mutableListOf<Subtitle>()
                val resolvedSkips = skipIntervals.toMutableList()

                for (apiUrl in candidateApiUrls) {
                    try {
                        val apiReq = Request.Builder()
                            .url(apiUrl)
                            .addHeader("Accept", "*/*")
                            .addHeader("X-Requested-With", "XMLHttpRequest")
                            .addHeader("Referer", embedUrl)
                            .addHeader("Origin", baseOrigin)
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .build()

                        val sourceResp = client.newCall(apiReq).await().use { it.body?.string().orEmpty() }

                        if (sourceResp.trim().startsWith("{")) {
                            val sourceJson = JSONObject(sourceResp)
                            val sourcesVal = sourceJson.opt("sources")

                            var candidateUrl = ""
                            when (sourcesVal) {
                                is JSONObject -> candidateUrl = sourcesVal.optString("file", "")
                                is org.json.JSONArray -> {
                                    for (i in 0 until sourcesVal.length()) {
                                        val item = sourcesVal.optJSONObject(i)
                                        val file = item?.optString("file", "").orEmpty()
                                        if (file.contains(".m3u8", ignoreCase = true) || file.contains(".mp4", ignoreCase = true)) {
                                            candidateUrl = file
                                            break
                                        }
                                    }
                                    if (candidateUrl.isEmpty() && sourcesVal.length() > 0) {
                                        candidateUrl = sourcesVal.optJSONObject(0)?.optString("file", "")
                                            ?: sourcesVal.optString(0, "")
                                    }
                                }
                                is String -> candidateUrl = sourcesVal
                            }

                            if (candidateUrl.isNotEmpty()) {
                                finalStreamUrl = candidateUrl
                            }

                            val tracksVal = sourceJson.optJSONArray("tracks")
                            if (tracksVal != null && subtitles.isEmpty()) {
                                for (i in 0 until tracksVal.length()) {
                                    val t = tracksVal.getJSONObject(i)
                                    val kind = t.optString("kind", "")
                                    if (kind == "captions" || kind.isEmpty()) {
                                        val subFile = t.optString("file", "")
                                        if (subFile.isNotEmpty()) {
                                            subtitles.add(
                                                Subtitle(
                                                    label = t.optString("label", "English"),
                                                    url = subFile,
                                                    isDefault = t.optBoolean("default", false),
                                                    isForced = false,
                                                    isSdh = false,
                                                    format = "VTT"
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (resolvedSkips.isEmpty()) {
                                val introObj = sourceJson.optJSONObject("intro")
                                if (introObj != null) {
                                    val start = introObj.optDouble("start", 0.0)
                                    val end = introObj.optDouble("end", 0.0)
                                    if (end > start) resolvedSkips.add(SkipInterval(start, end, "op"))
                                }
                                val outroObj = sourceJson.optJSONObject("outro")
                                if (outroObj != null) {
                                    val start = outroObj.optDouble("start", 0.0)
                                    val end = outroObj.optDouble("end", 0.0)
                                    if (end > start) resolvedSkips.add(SkipInterval(start, end, "ed"))
                                }
                            }

                            if (finalStreamUrl.isNotEmpty()) break
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Source API error at $apiUrl", e)
                    }
                }

                if (finalStreamUrl.isNotEmpty()) {
                    val headers = mapOf(
                        "Referer" to "$baseOrigin/",
                        "Origin" to baseOrigin,
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    )

                    val isDirectMp4 = finalStreamUrl.contains(".mp4", ignoreCase = true)
                    if (isDirectMp4) {
                        resultStreams.add(
                            VideoStream(
                                quality = "$prefix $serverName",
                                url = finalStreamUrl,
                                headers = headers,
                                isM3U8 = false,
                                subtitles = subtitles,
                                skipIntervals = resolvedSkips,
                                format = "MP4",
                                resolution = "Auto",
                                serverName = "$prefix $serverName"
                            )
                        )
                    } else {
                        val parsedVariants = parseM3U8(finalStreamUrl, headers, serverName, prefix, subtitles, resolvedSkips)
                        if (parsedVariants.isNotEmpty()) {
                            resultStreams.addAll(parsedVariants)
                        } else {
                            resultStreams.add(
                                VideoStream(
                                    quality = "$prefix $serverName",
                                    url = finalStreamUrl,
                                    headers = headers,
                                    isM3U8 = true,
                                    subtitles = subtitles,
                                    skipIntervals = resolvedSkips,
                                    format = "HLS",
                                    resolution = "Auto",
                                    serverName = "$prefix $serverName"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed resolving stream from embed: $embedUrl", e)
        }
        return resultStreams
    }

    private suspend fun parseM3U8(
        masterUrl: String,
        headers: Map<String, String>,
        serverName: String,
        prefix: String,
        subtitles: List<Subtitle>,
        skipIntervals: List<SkipInterval>
    ): List<VideoStream> {
        val reqBuilder = Request.Builder().url(masterUrl)
        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        val manifestText = try {
            client.newCall(reqBuilder.build()).await().use { it.body?.string().orEmpty() }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch master manifest: $masterUrl", e)
            return emptyList()
        }

        if (!manifestText.contains("#EXT-X-STREAM-INF")) {
            return emptyList()
        }

        val streams = mutableListOf<VideoStream>()
        val lines = manifestText.lines()
        var currentBandwidth: Long? = null
        var currentResolution: String? = null
        var currentCodecs: String? = null

        val bwRegex = Regex("""BANDWIDTH=(\d+)""")
        val resRegex = Regex("""RESOLUTION=(\d+x\d+)""")
        val codecRegex = Regex("""CODECS="([^"]+)"""")

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                currentBandwidth = bwRegex.find(line)?.groupValues?.get(1)?.toLongOrNull()
                currentResolution = resRegex.find(line)?.groupValues?.get(1)
                currentCodecs = codecRegex.find(line)?.groupValues?.get(1)
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                val variantUrl = try {
                    URI(masterUrl).resolve(line).toString()
                } catch (_: Exception) {
                    if (line.startsWith("http")) line else "${masterUrl.substringBeforeLast("/")}/$line"
                }

                val height = currentResolution?.substringAfter("x")?.toIntOrNull()
                val qualityLabel = when {
                    height != null && height >= 1080 -> "1080p"
                    height != null && height >= 720 -> "720p"
                    height != null && height >= 480 -> "480p"
                    height != null && height >= 360 -> "360p"
                    height != null -> "${height}p"
                    else -> "HD"
                }

                val estimatedSizeBytes = currentBandwidth?.let { bw ->
                    ((bw.toDouble() / 8.0) * 1440.0).toLong()
                }

                streams.add(
                    VideoStream(
                        quality = "$prefix $serverName ($qualityLabel)",
                        url = variantUrl,
                        headers = headers,
                        isM3U8 = true,
                        subtitles = subtitles,
                        skipIntervals = skipIntervals,
                        bitrate = currentBandwidth,
                        sizeInBytes = estimatedSizeBytes,
                        resolution = qualityLabel,
                        codec = currentCodecs,
                        format = "HLS",
                        serverName = "$prefix $serverName"
                    )
                )
                currentBandwidth = null
                currentResolution = null
                currentCodecs = null
            }
        }

        return streams
    }

    private fun normalizeUrl(url: String, base: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$base$url"
            else -> "https://$url"
        }
    }
}
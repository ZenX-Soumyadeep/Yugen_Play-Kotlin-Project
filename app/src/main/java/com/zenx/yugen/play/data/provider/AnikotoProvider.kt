package com.zenx.yugen.play.data.provider

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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLEncoder

class AnikotoProvider(
    private val client: OkHttpClient
) : AnimeProvider {

    override val name: String = "Anikoto"
    override val baseUrl: String = "https://anikoto.cz"

    private val tag = "YUGEN_PLAYER"

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val searchUrl = "$baseUrl/filter?keyword=$encodedQuery"

        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val html = try {
            client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            Log.e(tag, "Search request failed", e)
            return@withContext emptyList()
        }

        if (html.isEmpty()) return@withContext emptyList()

        val doc = Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()
        val items = doc.select(".flw-item, div.item, .film_list-wrap > div, .film-detail")

        items.forEach { item ->
            val aTag = item.selectFirst("a.poster, a.film-poster, a.dynamic-name, a[href*='/watch/']")
            val imgTag = item.selectFirst("img")
            val titleTag = item.selectFirst(".name, .film-name, h3.title, .film-name a")

            if (aTag != null && (titleTag != null || aTag.hasAttr("title"))) {
                val rawHref = aTag.attr("href").substringBefore("?")
                val url = if (rawHref.startsWith("http")) rawHref else "$baseUrl$rawHref"
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
            .build()

        val html = try {
            client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed fetching anime page: $animeUrl", e)
            return@withContext emptyList()
        }

        val doc = Jsoup.parse(html)
        val episodes = mutableListOf<Episode>()

        val animeId = doc.selectFirst("#syncData")?.attr("data-id")
            ?: doc.selectFirst("div[data-id]")?.attr("data-id")
            ?: doc.selectFirst("#watch-page")?.attr("data-id")
            ?: ""

        var epElements = doc.select(".episodes-list a, .ep-item a, ul.episodes li a, .ssl-item a, .episodes-ul a")

        if (epElements.isEmpty() && animeId.isNotEmpty()) {
            val ajaxUrls = listOf(
                "$baseUrl/ajax/v2/episode/list/$animeId",
                "$baseUrl/ajax/episode/list/$animeId",
                "$baseUrl/ajax/episode/list?id=$animeId"
            )

            for (ajaxUrl in ajaxUrls) {
                try {
                    val ajaxReq = Request.Builder()
                        .url(ajaxUrl)
                        .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                        .addHeader("Referer", animeUrl)
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()

                    val ajaxResp = client.newCall(ajaxReq).execute().use { it.body?.string().orEmpty() }
                    if (ajaxResp.isNotEmpty()) {
                        val ajaxHtml = if (ajaxResp.startsWith("{")) {
                            val json = JSONObject(ajaxResp)
                            json.optString("result", json.optString("html", ""))
                        } else {
                            ajaxResp
                        }

                        if (ajaxHtml.isNotEmpty()) {
                            val ajaxDoc = Jsoup.parse(ajaxHtml)
                            epElements = ajaxDoc.select("a.ep-item, a[data-id], .ssl-item a, li a")
                            if (epElements.isNotEmpty()) break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "AJAX episode fetch failed for: $ajaxUrl", e)
                }
            }
        }

        epElements.forEachIndexed { index, epElement ->
            val rawEpUrl = epElement.attr("href")
            val fullEpUrl = if (rawEpUrl.startsWith("http")) {
                rawEpUrl
            } else if (rawEpUrl.startsWith("/") && !rawEpUrl.startsWith("/#")) {
                "$baseUrl$rawEpUrl"
            } else {
                animeUrl
            }

            val title = epElement.text().trim().ifEmpty {
                epElement.attr("title").trim().ifEmpty { "Episode ${index + 1}" }
            }

            val dataIds = epElement.attr("data-ids")
                .ifEmpty { epElement.attr("data-sources") }
                .ifEmpty { epElement.attr("data-id") }
                .ifEmpty { epElement.attr("data-number") }
                .ifEmpty { animeId }

            val compoundId = "$fullEpUrl~~~$dataIds~~~${index + 1}"
            episodes.add(Episode(id = compoundId, title = title, number = (index + 1).toFloat()))
        }

        if (episodes.isEmpty()) {
            episodes.add(Episode(id = "$animeUrl~~~$animeId", title = "Full Movie / Episode 1", number = 1f))
        }

        episodes
    }

    override suspend fun extractStreams(episodeId: String, title: String): List<VideoStream> = withContext(Dispatchers.IO) {
        val parts = episodeId.split("~~~")
        val epUrl = parts[0]
        val serverParam = if (parts.size > 1) parts[1] else ""

        val candidateServerUrls = listOf(
            "$baseUrl/ajax/server/list?servers=$serverParam",
            "$baseUrl/ajax/server/list?id=$serverParam",
            "$baseUrl/ajax/v2/episode/servers?episodeId=$serverParam",
            "$baseUrl/ajax/episode/servers?id=$serverParam"
        )

        var serverHtml = ""
        for (serverUrl in candidateServerUrls) {
            try {
                val serverReq = Request.Builder()
                    .url(serverUrl)
                    .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                    .addHeader("Referer", epUrl)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val respStr = client.newCall(serverReq).execute().use { it.body?.string().orEmpty() }

                if (respStr.trim().startsWith("{")) {
                    val json = JSONObject(respStr)
                    val status = json.optInt("status", 200)
                    if (status in 200..299) {
                        val extracted = json.optString("html", json.optString("result", ""))
                            .ifEmpty { json.optString("data", "") }

                        if (extracted.contains("<") && extracted.contains(">")) {
                            serverHtml = extracted
                            break
                        }
                    }
                } else if (respStr.contains("<") && respStr.contains(">")) {
                    serverHtml = respStr
                    break
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed endpoint: $serverUrl", e)
            }
        }

        val serverDoc = Jsoup.parse(serverHtml)
        val serverElements = serverDoc.select(
            "[data-link-id], [data-id], [data-server-id], .server-item, .btn-server, .servers li, .item, .ps__-list .item"
        ).filter { !it.hasClass("download-icon") && !it.hasClass("nav-item") && !it.hasClass("tab-item") }

        Log.d(tag, "Found ${serverElements.size} server nodes. Executing bounded parallel extraction...")

        val semaphore = Semaphore(3)

        val deferredStreams = serverElements.map { serverElement ->
            async {
                semaphore.withPermit {
                    val serverId = serverElement.attr("data-link-id")
                        .ifEmpty { serverElement.attr("data-id") }
                        .ifEmpty { serverElement.attr("data-server-id") }

                    val serverName = serverElement.text().trim().ifEmpty { "Server" }
                    val typeStr = serverElement.closest(".type, .servers-sub, .servers-dub")?.attr("data-type")
                        ?: if (serverElement.parents().hasClass("servers-dub")) "dub" else "sub"
                    val prefix = if (typeStr.contains("dub", true)) "[DUB]" else "[SUB]"

                    val extractedList = mutableListOf<VideoStream>()

                    if (serverId.isNotEmpty()) {
                        try {
                            val candidateSourceUrls = listOf(
                                "$baseUrl/ajax/server?get=$serverId",
                                "$baseUrl/ajax/v2/episode/sources?id=$serverId",
                                "$baseUrl/ajax/episode/sources?id=$serverId"
                            )

                            for (sourceUrl in candidateSourceUrls) {
                                val embedReq = Request.Builder()
                                    .url(sourceUrl)
                                    .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                                    .addHeader("Referer", epUrl)
                                    .addHeader("X-Requested-With", "XMLHttpRequest")
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .build()

                                val embedResp = client.newCall(embedReq).execute().use { it.body?.string().orEmpty() }

                                if (embedResp.trim().startsWith("{")) {
                                    val json = JSONObject(embedResp)
                                    val resultObj = json.optJSONObject("result")
                                    val rawEmbedUrl = resultObj?.optString("url", "")
                                        ?: json.optString("link", "")
                                        ?: json.optString("url", "")

                                    val skipIntervals = mutableListOf<SkipInterval>()
                                    val skipDataObj = resultObj?.optJSONObject("skip_data")
                                    if (skipDataObj != null) {
                                        val introArr = skipDataObj.optJSONArray("intro")
                                        if (introArr != null && introArr.length() >= 2) {
                                            skipIntervals.add(SkipInterval(introArr.optDouble(0), introArr.optDouble(1), "op"))
                                        }
                                        val outroArr = skipDataObj.optJSONArray("outro")
                                        if (outroArr != null && outroArr.length() >= 2) {
                                            skipIntervals.add(SkipInterval(outroArr.optDouble(0), outroArr.optDouble(1), "ed"))
                                        }
                                    }

                                    if (rawEmbedUrl.isNotEmpty()) {
                                        val embedUrl = normalizeUrl(rawEmbedUrl, baseUrl)
                                        val resolved = resolveEmbedStreams(embedUrl, epUrl, serverName, prefix, skipIntervals)
                                        if (resolved.isNotEmpty()) {
                                            extractedList.addAll(resolved)
                                            break
                                        }
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

        val streams = deferredStreams.awaitAll().flatten()
        Log.d(tag, "Extraction complete. Found ${streams.size} playable streams.")
        return@withContext streams
    }

    private fun resolveEmbedStreams(
        embedUrl: String,
        referer: String,
        serverName: String,
        prefix: String,
        skipIntervals: List<SkipInterval>
    ): List<VideoStream> {
        val resultStreams = mutableListOf<VideoStream>()
        try {
            val uri = URI(embedUrl)
            val host = uri.host ?: "anikoto.cz"
            val scheme = uri.scheme ?: "https"
            val baseOrigin = "$scheme://$host"

            val pageBody = client.newCall(
                Request.Builder()
                    .url(embedUrl)
                    .addHeader("Referer", referer)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            ).execute().use { it.body?.string().orEmpty() }

            val m3u8Regex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
            val fileRegex = Regex("""["']?file["']?\s*:\s*["'](https?://[^"']+)["']""")

            var finalStreamUrl = m3u8Regex.find(pageBody)?.groupValues?.get(0).orEmpty()
            if (finalStreamUrl.isEmpty()) {
                finalStreamUrl = fileRegex.find(pageBody)?.groupValues?.get(1).orEmpty()
            }

            var dataId = Regex("""data-id="([^"]+)"""").find(pageBody)?.groupValues?.get(1).orEmpty()
            if (dataId.isEmpty()) {
                dataId = Regex("""data-video-id="([^"]+)"""").find(pageBody)?.groupValues?.get(1).orEmpty()
            }
            if (dataId.isEmpty()) {
                val pathSegments = uri.path.split("/").filter {
                    it.isNotEmpty() && it != "stream" && it != "s-2" && it != "sub" && it != "dub" && it != "hsub"
                }
                dataId = pathSegments.lastOrNull().orEmpty()
            }

            val subtitles = mutableListOf<Subtitle>()

            if (dataId.isNotEmpty()) {
                val candidateApiUrls = listOf(
                    "$baseOrigin/stream/getSources?id=$dataId",
                    "$baseOrigin/stream/getSourcesNew?id=$dataId",
                    "$baseOrigin/ajax/getSources?id=$dataId"
                )

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

                        val sourceResp = client.newCall(apiReq).execute().use { it.body?.string().orEmpty() }

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
                                        if (file.contains(".mp4", ignoreCase = true)) {
                                            candidateUrl = file
                                            break
                                        }
                                    }
                                    if (candidateUrl.isEmpty() && sourcesVal.length() > 0) {
                                        candidateUrl = sourcesVal.getJSONObject(0).optString("file", "")
                                    }
                                }
                                is String -> candidateUrl = sourcesVal
                            }

                            if (candidateUrl.isNotEmpty()) {
                                finalStreamUrl = candidateUrl
                            }

                            val tracksVal = sourceJson.optJSONArray("tracks")
                            if (tracksVal != null) {
                                for (i in 0 until tracksVal.length()) {
                                    val t = tracksVal.getJSONObject(i)
                                    val trackUrl = t.optString("file", "")
                                    val label = t.optString("label", "English")
                                    val isDefault = t.optBoolean("default", false)

                                    val lowerLabel = label.lowercase()
                                    val isForced = lowerLabel.contains("forced") || lowerLabel.contains("foreign")
                                    val isSdh = lowerLabel.contains("sdh") || lowerLabel.contains("cc")
                                    val subFormat = when {
                                        trackUrl.endsWith(".vtt", ignoreCase = true) -> "VTT"
                                        trackUrl.endsWith(".ass", ignoreCase = true) -> "ASS"
                                        trackUrl.endsWith(".srt", ignoreCase = true) -> "SRT"
                                        else -> "VTT"
                                    }

                                    if (trackUrl.isNotEmpty()) {
                                        subtitles.add(
                                            Subtitle(
                                                label = label,
                                                url = trackUrl,
                                                isDefault = isDefault,
                                                isForced = isForced,
                                                isSdh = isSdh,
                                                format = subFormat
                                            )
                                        )
                                    }
                                }
                            }

                            if (finalStreamUrl.isNotEmpty()) break
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Source API error at $apiUrl", e)
                    }
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
                    val sizeInBytes = fetchContentLength(finalStreamUrl, headers)
                    resultStreams.add(
                        VideoStream(
                            quality = "$prefix $serverName",
                            url = finalStreamUrl,
                            headers = headers,
                            isM3U8 = false,
                            subtitles = subtitles,
                            skipIntervals = skipIntervals,
                            sizeInBytes = sizeInBytes,
                            format = "MP4",
                            resolution = "1080p",
                            serverName = "$prefix $serverName"
                        )
                    )
                } else {
                    val parsedVariants = parseM3U8(
                        masterUrl = finalStreamUrl,
                        headers = headers,
                        serverName = serverName,
                        prefix = prefix,
                        subtitles = subtitles,
                        skipIntervals = skipIntervals
                    )

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
                                skipIntervals = skipIntervals,
                                format = "HLS",
                                resolution = "Auto",
                                serverName = "$prefix $serverName"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed resolving stream from embed: $embedUrl", e)
        }
        return resultStreams
    }

    private fun parseM3U8(
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
            client.newCall(reqBuilder.build()).execute().use { it.body?.string().orEmpty() }
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
                } catch (e: Exception) {
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

                // Calculate estimated file size for ~24 min episode (1440 seconds)
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

    private fun fetchContentLength(url: String, headers: Map<String, String>): Long? {
        return try {
            val reqBuilder = Request.Builder().url(url).head()
            headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            client.newCall(reqBuilder.build()).execute().use { resp ->
                resp.header("Content-Length")?.toLongOrNull()
            }
        } catch (e: Exception) {
            null
        }
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
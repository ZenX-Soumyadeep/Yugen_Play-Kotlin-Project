package com.zenx.yugen.play.util

import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object CastProxy {
    private const val TAG = "CastProxy"
    private const val MAX_PAYLOAD_SIZE = 4 * 1024 * 1024 // 4 MB safety limit
    private const val MAX_CONCURRENT_CONNECTIONS = 30 // Bounded to prevent CPU ANRs from HLS chunk spam

    private var serverSocket: ServerSocket? = null
    private var executor: ExecutorService? = null
    private var listenerThread: Thread? = null

    @Volatile
    private var isRunning = false

    @Volatile
    private var port = 0

    @Volatile
    private var globalReferer = "https://megaplay.buzz/"

    private val lock = Any()

    fun start(referer: String) {
        synchronized(lock) {
            globalReferer = referer
            if (isRunning && port > 0) return

            stopInternal()

            try {
                // Fixed: Use SynchronousQueue so worker threads scale dynamically up to MAX_CONCURRENT_CONNECTIONS
                val pool = ThreadPoolExecutor(
                    4, MAX_CONCURRENT_CONNECTIONS,
                    60L, TimeUnit.SECONDS,
                    SynchronousQueue<Runnable>()
                )
                executor = pool

                val socket = ServerSocket(0)
                serverSocket = socket
                port = socket.localPort
                isRunning = true

                // Dedicated thread for socket accept loop to prevent blocking worker threads
                val listener = Thread({
                    while (isRunning && !socket.isClosed) {
                        try {
                            val client = socket.accept()
                            client.soTimeout = 10000
                            try {
                                pool.execute { handleClient(client) }
                            } catch (e: RejectedExecutionException) {
                                try {
                                    client.getOutputStream().write("HTTP/1.1 503 Service Unavailable\r\n\r\n".toByteArray())
                                    client.close()
                                } catch (_: Exception) {}
                            }
                        } catch (_: SocketException) {
                            break
                        } catch (e: Exception) {
                            if (isRunning) {
                                Log.e(TAG, "Error accepting client connection", e)
                            }
                            break
                        }
                    }
                }, "CastProxy-Acceptor").apply {
                    isDaemon = true
                    start()
                }
                listenerThread = listener
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize relay server", e)
                stopInternal()
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            stopInternal()
        }
    }

    private fun stopInternal() {
        isRunning = false
        port = 0

        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null

        listenerThread?.interrupt()
        listenerThread = null

        try { executor?.shutdownNow() } catch (_: Exception) {}
        executor = null
    }

    fun getProxyUrl(originalUrl: String): String {
        val currentPort = port
        if (!isRunning || currentPort <= 0) return originalUrl

        val ip = getLocalIp() ?: return originalUrl
        val encoded = URLEncoder.encode(originalUrl, "UTF-8")
        return "http://$ip:$currentPort/proxy?url=$encoded"
    }

    private fun readBoundedStream(input: InputStream, limit: Int): ByteArray? {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var totalRead = 0

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            totalRead += read
            if (totalRead > limit) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun handleClient(client: Socket) {
        val currentPort = port
        val activeReferer = globalReferer

        if (!isRunning || currentPort <= 0) {
            try { client.close() } catch (_: Exception) {}
            return
        }

        // Issue 10.1 Fix: Ensures the client socket is always closed natively
        client.use { safeClient ->
            try {
                val reader = BufferedReader(InputStreamReader(safeClient.getInputStream()))
                val requestLine = reader.readLine() ?: return

                if (requestLine.startsWith("OPTIONS")) {
                    val out = safeClient.getOutputStream()
                    out.write("HTTP/1.1 200 OK\r\n".toByteArray())
                    out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                    out.write("Access-Control-Allow-Methods: GET, OPTIONS, HEAD\r\n".toByteArray())
                    out.write("Access-Control-Allow-Headers: *\r\n".toByteArray())
                    out.write("Content-Length: 0\r\n\r\n".toByteArray())
                    out.flush()
                    return
                }

                if (!requestLine.startsWith("GET")) return

                val path = requestLine.split(" ").getOrNull(1) ?: return
                if (!path.startsWith("/proxy?url=")) return

                var rangeHeader: String? = null
                while (true) {
                    val header = reader.readLine()
                    if (header.isNullOrBlank()) break
                    if (header.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = header.substringAfter(":").trim()
                    }
                }

                val targetUrl = URLDecoder.decode(path.substringAfter("url="), "UTF-8")
                val out = safeClient.getOutputStream()

                if (targetUrl.startsWith("file://") || targetUrl.startsWith("file:")) {
                    val filePath = targetUrl.removePrefix("file://").removePrefix("file:")
                    val file = File(filePath)
                    if (file.exists() && file.length() <= MAX_PAYLOAD_SIZE) {
                        val bytes = file.readBytes()
                        out.write("HTTP/1.1 200 OK\r\n".toByteArray())
                        out.write("Content-Type: text/vtt; charset=utf-8\r\n".toByteArray())
                        out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                        out.write("Access-Control-Allow-Headers: *\r\n".toByteArray())
                        out.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray())
                        out.write(bytes)
                        out.flush()
                    }
                    return
                }

                val connection = URL(targetUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.setRequestProperty("Referer", activeReferer)
                connection.setRequestProperty("Origin", activeReferer)
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                if (rangeHeader != null) connection.setRequestProperty("Range", rangeHeader)

                val status = connection.responseCode
                val statusText = when (status) {
                    200 -> "OK"
                    206 -> "Partial Content"
                    403 -> "Forbidden"
                    404 -> "Not Found"
                    413 -> "Payload Too Large"
                    500 -> "Internal Server Error"
                    503 -> "Service Unavailable"
                    else -> "Error"
                }

                val upstreamStream = try {
                    if (status >= 400) connection.errorStream ?: connection.inputStream else connection.inputStream
                } catch (_: Exception) {
                    null
                }

                if (upstreamStream == null) {
                    out.write("HTTP/1.1 $status $statusText\r\nContent-Length: 0\r\n\r\n".toByteArray())
                    out.flush()
                    connection.disconnect()
                    return
                }

                if (targetUrl.contains(".m3u8")) {
                    val rawBytes = upstreamStream.use { readBoundedStream(it, MAX_PAYLOAD_SIZE) }
                    connection.disconnect()

                    if (rawBytes == null) {
                        out.write("HTTP/1.1 413 Payload Too Large\r\n\r\n".toByteArray())
                        out.flush()
                        return
                    }

                    val hostIp = getLocalIp() ?: "127.0.0.1"
                    val manifest = rawBytes.toString(Charsets.UTF_8)
                    val uriAttrRegex = Regex("""URI="([^"]+)"""")

                    val rewritten = manifest.lines().joinToString("\n") { line ->
                        if (line.isBlank()) {
                            line
                        } else if (line.startsWith("#")) {
                            if (line.contains("URI=\"")) {
                                line.replace(uriAttrRegex) { matchResult ->
                                    val subUri = matchResult.groupValues[1]
                                    val absoluteUrl = if (subUri.startsWith("http")) subUri else URL(URL(targetUrl), subUri).toString()
                                    // CDN fix: re-point stale `*.akirax.buzz` segment hosts at the live CDN.
                                    val fixedUrl = CdnHostRewriter.rewriteSegmentHost(absoluteUrl)
                                    "URI=\"http://$hostIp:$currentPort/proxy?url=${URLEncoder.encode(fixedUrl, "UTF-8")}\""
                                }
                            } else {
                                line
                            }
                        } else {
                            val absoluteUrl = if (line.startsWith("http")) line else URL(URL(targetUrl), line).toString()
                            // CDN fix: re-point stale `*.akirax.buzz` segment hosts at the live CDN.
                            val fixedUrl = CdnHostRewriter.rewriteSegmentHost(absoluteUrl)
                            "http://$hostIp:$currentPort/proxy?url=${URLEncoder.encode(fixedUrl, "UTF-8")}"
                        }
                    }
                    val bytes = rewritten.toByteArray()
                    out.write("HTTP/1.1 $status $statusText\r\n".toByteArray())
                    out.write("Content-Type: application/vnd.apple.mpegurl\r\n".toByteArray())
                    out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                    out.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray())
                    out.write(bytes)
                } else if (targetUrl.contains(".vtt", ignoreCase = true)) {
                    val bytes = upstreamStream.use { readBoundedStream(it, MAX_PAYLOAD_SIZE) }
                    connection.disconnect()

                    if (bytes == null) {
                        out.write("HTTP/1.1 413 Payload Too Large\r\n\r\n".toByteArray())
                        out.flush()
                        return
                    }
                    out.write("HTTP/1.1 $status $statusText\r\n".toByteArray())
                    out.write("Content-Type: text/vtt; charset=utf-8\r\n".toByteArray())
                    out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                    out.write("Access-Control-Allow-Headers: *\r\n".toByteArray())
                    out.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray())
                    out.write(bytes)
                } else {
                    out.write("HTTP/1.1 $status $statusText\r\n".toByteArray())
                    val contentType = connection.contentType
                    if (contentType != null) out.write("Content-Type: $contentType\r\n".toByteArray())

                    out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                    out.write("Accept-Ranges: bytes\r\n".toByteArray())

                    val cl = connection.getHeaderField("Content-Length")
                    if (cl != null) out.write("Content-Length: $cl\r\n".toByteArray())

                    val cr = connection.getHeaderField("Content-Range")
                    if (cr != null) out.write("Content-Range: $cr\r\n".toByteArray())

                    out.write("\r\n".toByteArray())

                    upstreamStream.use { input ->
                        input.copyTo(out, bufferSize = 128 * 1024)
                    }
                    connection.disconnect()
                }
                out.flush()
            } catch (_: Exception) {}
        }
    }

    fun getLocalIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val preferredInterfaces = interfaces.filter { iface ->
                iface.isUp && !iface.isLoopback && !iface.isPointToPoint &&
                        !iface.name.contains("p2p", ignoreCase = true) &&
                        !iface.name.contains("tun", ignoreCase = true) &&
                        !iface.name.contains("tap", ignoreCase = true) &&
                        !iface.name.contains("ap", ignoreCase = true) &&
                        !iface.name.contains("dummy", ignoreCase = true) &&
                        (iface.name.startsWith("wlan", ignoreCase = true) ||
                                iface.name.startsWith("eth", ignoreCase = true) ||
                                iface.name.startsWith("en", ignoreCase = true))
            }

            for (iface in preferredInterfaces) {
                for (addr in iface.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address && !addr.isLinkLocalAddress) {
                        return addr.hostAddress
                    }
                }
            }

            for (iface in interfaces.filter { it.isUp && !it.isLoopback && !it.isPointToPoint }) {
                for (addr in iface.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address && !addr.isLinkLocalAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
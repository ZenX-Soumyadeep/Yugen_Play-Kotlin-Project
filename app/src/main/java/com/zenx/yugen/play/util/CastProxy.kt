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
import java.util.concurrent.Executors

object CastProxy {
    private const val TAG = "CastProxy"
    private const val MAX_PAYLOAD_SIZE = 4 * 1024 * 1024 // 4 MB safety limit

    private var serverSocket: ServerSocket? = null
    private var executor: ExecutorService? = null

    @Volatile
    private var isRunning = false

    @Volatile
    private var port = 0

    @Volatile
    private var globalReferer = "https://megaplay.buzz/"

    private val lock = Any()

    @Synchronized
    fun start(referer: String) {
        synchronized(lock) {
            globalReferer = referer
            if (isRunning && port > 0) return

            stopInternal()

            try {
                val pool = Executors.newCachedThreadPool()
                executor = pool

                val socket = ServerSocket(0)
                serverSocket = socket
                port = socket.localPort
                isRunning = true

                pool.execute {
                    while (isRunning && !socket.isClosed) {
                        try {
                            val client = socket.accept()
                            client.soTimeout = 10000
                            pool.execute { handleClient(client) }
                        } catch (_: SocketException) {
                            break
                        } catch (e: Exception) {
                            if (isRunning) {
                                Log.e(TAG, "Error accepting client connection", e)
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize relay server", e)
                stopInternal()
            }
        }
    }

    @Synchronized
    fun stop() {
        synchronized(lock) {
            stopInternal()
        }
    }

    private fun stopInternal() {
        isRunning = false
        port = 0

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        try {
            executor?.shutdownNow()
        } catch (_: Exception) {}
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

        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val requestLine = reader.readLine() ?: return

            if (requestLine.startsWith("OPTIONS")) {
                val out = client.getOutputStream()
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
            val out = client.getOutputStream()

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

            if (targetUrl.contains(".m3u8")) {
                val rawBytes = readBoundedStream(connection.inputStream, MAX_PAYLOAD_SIZE)
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
                                "URI=\"http://$hostIp:$currentPort/proxy?url=${URLEncoder.encode(absoluteUrl, "UTF-8")}\""
                            }
                        } else {
                            line
                        }
                    } else {
                        val absoluteUrl = if (line.startsWith("http")) line else URL(URL(targetUrl), line).toString()
                        "http://$hostIp:$currentPort/proxy?url=${URLEncoder.encode(absoluteUrl, "UTF-8")}"
                    }
                }
                val bytes = rewritten.toByteArray()
                out.write("HTTP/1.1 $status $statusText\r\n".toByteArray())
                out.write("Content-Type: application/vnd.apple.mpegurl\r\n".toByteArray())
                out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                out.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray())
                out.write(bytes)
            } else if (targetUrl.contains(".vtt", ignoreCase = true)) {
                val bytes = readBoundedStream(connection.inputStream, MAX_PAYLOAD_SIZE)
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
                connection.inputStream.copyTo(out, bufferSize = 128 * 1024)
            }
            out.flush()
        } catch (_: Exception) {
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    fun getLocalIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()

            // L-15: Prioritize Wi-Fi and Ethernet interfaces, ignoring P2P, hotspot AP, and VPNs
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

            // Fallback: Any non-loopback, non-point-to-point IPv4
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
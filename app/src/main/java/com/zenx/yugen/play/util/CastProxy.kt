package com.zenx.yugen.play.util

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.Executors

object CastProxy {
    private const val TAG = "CastProxy"
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false
    private var port = 0
    private var globalReferer = "https://megaplay.buzz/"

    private val executor = Executors.newCachedThreadPool()

    @Synchronized
    fun start(referer: String) {
        if (isRunning) return
        globalReferer = referer

        try {
            serverSocket = ServerSocket(0)
            port = serverSocket!!.localPort
            isRunning = true

            executor.execute {
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        client.soTimeout = 10000
                        executor.execute { handleClient(client) }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local relay", e)
            isRunning = false
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    fun getProxyUrl(originalUrl: String): String {
        val ip = getLocalIp() ?: return originalUrl
        val encoded = URLEncoder.encode(originalUrl, "UTF-8")
        return "http://$ip:$port/proxy?url=$encoded"
    }

    private fun handleClient(client: Socket) {
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
                if (file.exists()) {
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

            connection.setRequestProperty("Referer", globalReferer)
            connection.setRequestProperty("Origin", globalReferer)
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            if (rangeHeader != null) connection.setRequestProperty("Range", rangeHeader)

            val status = connection.responseCode

            if (targetUrl.contains(".m3u8")) {
                val hostIp = getLocalIp() ?: "127.0.0.1"
                val manifest = connection.inputStream.bufferedReader().readText()
                val uriAttrRegex = Regex("""URI="([^"]+)"""")

                val rewritten = manifest.lines().joinToString("\n") { line ->
                    if (line.isBlank()) {
                        line
                    } else if (line.startsWith("#")) {
                        if (line.contains("URI=\"")) {
                            line.replace(uriAttrRegex) { matchResult ->
                                val subUri = matchResult.groupValues[1]
                                val absoluteUrl = if (subUri.startsWith("http")) subUri else URL(URL(targetUrl), subUri).toString()
                                "URI=\"http://$hostIp:$port/proxy?url=${URLEncoder.encode(absoluteUrl, "UTF-8")}\""
                            }
                        } else {
                            line
                        }
                    } else {
                        val absoluteUrl = if (line.startsWith("http")) line else URL(URL(targetUrl), line).toString()
                        "http://$hostIp:$port/proxy?url=${URLEncoder.encode(absoluteUrl, "UTF-8")}"
                    }
                }
                val bytes = rewritten.toByteArray()
                out.write("HTTP/1.1 200 OK\r\n".toByteArray())
                out.write("Content-Type: application/vnd.apple.mpegurl\r\n".toByteArray())
                out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                out.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray())
                out.write(bytes)
            } else if (targetUrl.contains(".vtt", ignoreCase = true)) {
                val bytes = connection.inputStream.readBytes()
                out.write("HTTP/1.1 200 OK\r\n".toByteArray())
                out.write("Content-Type: text/vtt; charset=utf-8\r\n".toByteArray())
                out.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                out.write("Access-Control-Allow-Headers: *\r\n".toByteArray())
                out.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray())
                out.write(bytes)
            } else {
                out.write("HTTP/1.1 $status OK\r\n".toByteArray())
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
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
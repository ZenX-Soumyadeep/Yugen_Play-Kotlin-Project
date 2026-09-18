package com.zenx.yugen.play.ui.tv.auth

import android.net.Uri
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class TvAuthServer(
    private val onTokenReceived: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    var serverPort: Int = 8765
        private set

    companion object {
        fun getLocalIpAddress(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
                for (nif in interfaces) {
                    if (!nif.isUp || nif.isLoopback) continue
                    val addresses = nif.inetAddresses
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (host != null && !host.startsWith("127.")) {
                                return host
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return null
        }
    }

    fun getServerUrl(): String? {
        val ip = getLocalIpAddress() ?: return null
        return "http://$ip:$serverPort"
    }

    fun start(scope: CoroutineScope): String? {
        stop()
        try {
            serverSocket = ServerSocket(serverPort)
        } catch (_: Exception) {
            // Try fallback port
            serverPort = 8766
            serverSocket = ServerSocket(serverPort)
        }

        val url = getServerUrl()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(socket)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
        return url
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        job?.cancel()
        job = null
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                val writer = PrintWriter(client.getOutputStream(), true)

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val path = parts[1]

                // Read HTTP Headers
                var contentLength = 0
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    if (line.lowercase().startsWith("content-length:")) {
                        contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                if (method.equals("GET", ignoreCase = true) && (path == "/" || path.startsWith("/?"))) {
                    sendHtmlResponse(writer)
                } else if (method.equals("POST", ignoreCase = true) && path.startsWith("/submit-token")) {
                    if (contentLength !in 1..16384) {
                        sendJsonResponse(writer, 400, """{"success":false,"error":"Invalid payload size"}""")
                        return
                    }
                    val bodyBuilder = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = reader.read(bodyBuilder, read, contentLength - read)
                        if (r == -1) break
                        read += r
                    }
                    val body = String(bodyBuilder, 0, read)
                    val rawToken = parseTokenFromBody(body)
                    val cleanToken = cleanTokenString(rawToken)

                    if (cleanToken.isNotBlank()) {
                        sendJsonResponse(writer, 200, """{"success":true,"message":"Token received"}""")
                        onTokenReceived(cleanToken)
                    } else {
                        sendJsonResponse(writer, 400, """{"success":false,"error":"Token is empty or invalid"}""")
                    }
                } else {
                    send404Response(writer)
                }
            } catch (_: Exception) {}
        }
    }

    private fun cleanTokenString(raw: String): String {
        var token = raw.trim()
        if (token.contains("access_token=")) {
            val after = token.substringAfter("access_token=")
            token = after.substringBefore("&").substringBefore(" ").trim()
        }
        if (token.contains("#")) {
            val hashPart = token.substringAfter("#")
            if (hashPart.contains("access_token=")) {
                token = hashPart.substringAfter("access_token=").substringBefore("&").trim()
            }
        }
        return token
    }

    private fun parseTokenFromBody(body: String): String {
        try {
            if (body.startsWith("{")) {
                val json = JSONObject(body)
                if (json.has("token")) {
                    return json.getString("token")
                }
            }
        } catch (_: Exception) {}

        if (body.contains("token=")) {
            val encoded = body.substringAfter("token=").substringBefore("&")
            return try {
                URLDecoder.decode(encoded, "UTF-8")
            } catch (_: Exception) {
                encoded
            }
        }
        return body.trim()
    }

    private fun sendHtmlResponse(writer: PrintWriter) {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>YugenPlay TV • AniList Login</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                    body {
                        background-color: #0b0b0f;
                        color: #ffffff;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        padding: 24px 16px;
                    }
                    .card {
                        background: #15151e;
                        border: 1px solid rgba(255, 255, 255, 0.12);
                        border-radius: 20px;
                        max-width: 440px;
                        width: 100%;
                        padding: 32px 24px;
                        box-shadow: 0 16px 36px rgba(0, 0, 0, 0.6);
                    }
                    .logo-row {
                        display: flex;
                        align-items: center;
                        gap: 12px;
                        margin-bottom: 20px;
                    }
                    .badge {
                        background: linear-gradient(135deg, #8B5CF6, #3DB4F2);
                        color: white;
                        font-size: 11px;
                        font-weight: 800;
                        padding: 4px 10px;
                        border-radius: 100px;
                        letter-spacing: 0.5px;
                        text-transform: uppercase;
                    }
                    h1 { font-size: 24px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 8px; }
                    p.sub { font-size: 14px; color: rgba(255, 255, 255, 0.65); line-height: 1.5; margin-bottom: 24px; }
                    .step-card {
                        background: rgba(255, 255, 255, 0.04);
                        border: 1px solid rgba(255, 255, 255, 0.08);
                        border-radius: 14px;
                        padding: 16px;
                        margin-bottom: 16px;
                    }
                    .step-header {
                        font-size: 13px;
                        font-weight: 700;
                        color: #A78BFA;
                        margin-bottom: 8px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .btn {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        width: 100%;
                        padding: 14px;
                        border-radius: 12px;
                        font-size: 15px;
                        font-weight: 700;
                        cursor: pointer;
                        text-decoration: none;
                        border: none;
                        transition: all 0.2s ease;
                    }
                    .btn-auth {
                        background: #3DB4F2;
                        color: #0b0b0f;
                    }
                    .btn-auth:hover { background: #28a7e8; }
                    .btn-submit {
                        background: #8B5CF6;
                        color: white;
                        margin-top: 12px;
                    }
                    .btn-submit:hover { background: #7C3AED; }
                    textarea {
                        width: 100%;
                        background: rgba(0, 0, 0, 0.35);
                        border: 1px solid rgba(255, 255, 255, 0.15);
                        border-radius: 10px;
                        padding: 12px;
                        color: white;
                        font-size: 13px;
                        resize: vertical;
                        outline: none;
                    }
                    textarea:focus { border-color: #8B5CF6; }
                    #status {
                        margin-top: 16px;
                        padding: 12px;
                        border-radius: 10px;
                        font-size: 14px;
                        font-weight: 600;
                        display: none;
                        text-align: center;
                    }
                    .status-success { background: rgba(16, 185, 129, 0.2); color: #34D399; border: 1px solid rgba(16, 185, 129, 0.4); display: block !important; }
                    .status-error { background: rgba(239, 68, 68, 0.2); color: #F87171; border: 1px solid rgba(239, 68, 68, 0.4); display: block !important; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo-row">
                        <span class="badge">TV Connect</span>
                    </div>
                    <h1>Connect to AniList</h1>
                    <p class="sub">Link your AniList account to sync your watchlist, bookmarks, and watch progress with your TV.</p>

                    <div class="step-card">
                        <div class="step-header">Step 1: Authorize</div>
                        <a href="https://anilist.co/api/v2/oauth/authorize?client_id=48327&response_type=token" target="_blank" class="btn btn-auth">
                            1. Open AniList Authorization ↗
                        </a>
                    </div>

                    <div class="step-card">
                        <div class="step-header">Step 2: Paste Token & Send</div>
                        <textarea id="tokenBox" rows="3" placeholder="Paste the token or redirected web address here..."></textarea>
                        <button class="btn btn-submit" id="sendBtn" onclick="submitToken()">2. Send to TV</button>
                    </div>

                    <div id="status"></div>
                </div>

                <script>
                    function submitToken() {
                        const input = document.getElementById('tokenBox').value.trim();
                        const statusDiv = document.getElementById('status');
                        const sendBtn = document.getElementById('sendBtn');

                        if (!input) {
                            statusDiv.className = 'status-error';
                            statusDiv.textContent = 'Please paste your token first.';
                            return;
                        }

                        sendBtn.disabled = true;
                        sendBtn.textContent = 'Sending...';

                        fetch('/submit-token', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ token: input })
                        })
                        .then(res => res.json())
                        .then(data => {
                            if (data.success) {
                                statusDiv.className = 'status-success';
                                statusDiv.innerHTML = '✓ Successfully Sent!<br><span style="font-size: 12px; opacity: 0.85;">Your TV is now logged in. You can close this tab.</span>';
                                sendBtn.textContent = 'Done!';
                            } else {
                                statusDiv.className = 'status-error';
                                statusDiv.textContent = data.error || 'Failed to submit token.';
                                sendBtn.disabled = false;
                                sendBtn.textContent = '2. Send to TV';
                            }
                        })
                        .catch(err => {
                            statusDiv.className = 'status-error';
                            statusDiv.textContent = 'Network error. Make sure your phone is connected to the same Wi-Fi as your TV.';
                            sendBtn.disabled = false;
                            sendBtn.textContent = '2. Send to TV';
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: text/html; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        writer.write(html)
        writer.flush()
    }

    private fun sendJsonResponse(writer: PrintWriter, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $code OK\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        writer.write(json)
        writer.flush()
    }

    private fun send404Response(writer: PrintWriter) {
        writer.print("HTTP/1.1 404 Not Found\r\n")
        writer.print("Content-Length: 0\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
    }
}

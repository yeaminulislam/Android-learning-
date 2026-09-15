package com.shieldbrowser.app.adblock

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Downloads fresh filter lists from the internet and stores them in
 * filesDir/filters_updated/, where [AdBlockEngine] picks them up on reload.
 */
object FilterUpdater {

    private const val UPDATE_DIR = "filters_updated"
    private const val MAX_BYTES = 25 * 1024 * 1024   // 25 MB safety cap per list

    private val SOURCES = listOf(
        "hosts.txt" to
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
        "easylist.txt" to
                "https://easylist-downloads.adblockplus.org/easylist.txt",
        "easyprivacy.txt" to
                "https://easylist-downloads.adblockplus.org/easyprivacy.txt",
        "annoyances.txt" to
                "https://secure.fanboy.co.nz/fanboy-annoyance.txt"
    )

    interface Callback {
        fun onResult(success: Boolean, message: String)
    }

    fun update(context: Context, callback: Callback) {
        Thread {
            val dir = File(context.filesDir, UPDATE_DIR)
            if (!dir.exists()) dir.mkdirs()
            var downloaded = 0
            val errors = StringBuilder()
            for ((name, url) in SOURCES) {
                try {
                    val body = download(url)
                    if (body != null && body.length > 100) {
                        File(dir, name).writeText(body)
                        downloaded++
                    } else {
                        errors.append(name).append(' ')
                    }
                } catch (e: Exception) {
                    errors.append(name).append(' ')
                }
            }
            val ok = downloaded > 0
            if (ok) {
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                File(dir, "VERSION.txt").writeText(stamp)
                AdBlockEngine.reload(context)
            }
            val msg = if (ok) {
                "Downloaded $downloaded/${SOURCES.size} lists" +
                        if (errors.isNotEmpty()) " (failed: $errors)" else ""
            } else {
                "All downloads failed"
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback.onResult(ok, msg)
            }
        }.start()
    }

    private fun download(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ShieldBrowser/1.0")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(64 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_BYTES) break
                    out.write(buf, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

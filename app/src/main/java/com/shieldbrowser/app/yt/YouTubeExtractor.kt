package com.shieldbrowser.app.yt

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.URLDecoder

/**
 * Extracts downloadable media streams from YouTube videos.
 *
 * Strategy (belt & suspenders):
 *  1. Fetch the watch page (innertube API key, player JS url, embedded JSON).
 *  2. Try the Innertube ANDROID client — usually returns direct, unciphered URLs.
 *  3. Fall back to the streaming data embedded in the watch page.
 *  4. Resolve signature-ciphered and n-throttled URLs with [YoutubeJsEngine].
 */
object YouTubeExtractor {

    private const val TAG = "YouTubeExtractor"
    private const val ANDROID_CLIENT_VERSION = "19.09.37"
    private const val TIMEOUT_MS = 25_000
    private const val MAX_BYTES = 4 * 1024 * 1024
    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    data class Stream(
        val itag: Int,
        val qualityLabel: String,
        val container: String,
        val bitrate: Long,
        val contentLength: Long,     // -1 = unknown
        val url: String,
        val hasAudio: Boolean,
        val hasVideo: Boolean
    )

    data class Result(
        val videoId: String,
        val title: String,
        val author: String,
        val streams: List<Stream>,
        val error: String? = null
    ) {
        val ok: Boolean get() = error == null && streams.isNotEmpty()
    }

    interface Callback {
        fun onResult(result: Result)
    }

    // ------------------------------------------------------------------
    // video id / public entry points
    // ------------------------------------------------------------------

    fun videoIdOf(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        val patterns = listOf(
            Regex("[?&]v=([A-Za-z0-9_-]{11})"),
            Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
            Regex("/shorts/([A-Za-z0-9_-]{11})"),
            Regex("/embed/([A-Za-z0-9_-]{11})"),
            Regex("/live/([A-Za-z0-9_-]{11})")
        )
        for (p in patterns) {
            p.find(url)?.let { return it.groupValues[1] }
        }
        return null
    }

    fun looksLikeWatchUrl(url: String?): Boolean = videoIdOf(url) != null

    /** Starts extraction on a background thread; result is posted on the main thread. */
    fun extract(pageUrl: String, engine: YoutubeJsEngine?, callback: Callback) {
        val id = videoIdOf(pageUrl)
        Thread {
            val result = try {
                if (id == null) {
                    errorResult("", "invalid url")
                } else {
                    runExtraction(id, engine)
                }
            } catch (e: Exception) {
                errorResult(id ?: "", e.message ?: "error")
            }
            Handler(Looper.getMainLooper()).post { callback.onResult(result) }
        }.start()
    }

    // ------------------------------------------------------------------
    // extraction pipeline
    // ------------------------------------------------------------------

    private class RawStream(
        val itag: Int,
        val qualityLabel: String,
        val mime: String,
        val bitrate: Long,
        val length: Long,
        val url: String?,
        val cipher: String?,
        val hasAudio: Boolean,
        val hasVideo: Boolean
    )

    private fun runExtraction(videoId: String, engine: YoutubeJsEngine?): Result {
        val page = httpGet("https://www.youtube.com/watch?v=$videoId&hl=en")
            ?: return errorResult(videoId, "network")

        val apiKey = firstGroup(page, Regex("\"INNERTUBE_API_KEY\":\"([^\"]+)\""))
        val clientVersion =
            firstGroup(page, Regex("\"INNERTUBE_CLIENT_VERSION\":\"([^\"]+)\""))
        val playerJsPath = extractPlayerJsPath(page)
        val initial = extractInitialPlayerResponse(page)

        var title = videoTitle(initial) ?: "YouTube video $videoId"
        var author = videoAuthor(initial) ?: ""

        // 2) Innertube ANDROID client first (usually direct URLs)
        var raw: List<RawStream> = emptyList()
        if (!apiKey.isNullOrEmpty()) {
            val meta = arrayOf(title, author)
            raw = innertubeStreams(
                videoId, apiKey, clientVersion ?: ANDROID_CLIENT_VERSION, meta
            ) ?: emptyList()
            if (title.isBlank() && meta[0].isNotBlank()) title = meta[0]
            if (author.isBlank() && meta[1].isNotBlank()) author = meta[1]
        }

        // 3) fallback: streams embedded in the page
        if (raw.isEmpty() && initial != null) {
            raw = parseStreamingData(initial)
        }
        if (raw.isEmpty()) return errorResult(videoId, "no streams")

        // 4) resolve to final downloadable URLs
        val resolved = resolveStreams(raw, playerJsPath, engine)
        if (resolved.isEmpty()) return errorResult(videoId, "unresolvable")

        return Result(videoId, title, author, sortAndDedup(resolved), null)
    }

    /** Innertube player API with the Android client context. */
    private fun innertubeStreams(
        videoId: String,
        apiKey: String,
        clientVersion: String,
        metaOut: Array<String>
    ): List<RawStream>? {
        val body = "{\n" +
                "  \"context\": {\n" +
                "    \"client\": {\n" +
                "      \"clientName\": \"ANDROID\",\n" +
                "      \"clientVersion\": \"$ANDROID_CLIENT_VERSION\",\n" +
                "      \"androidSdkVersion\": 30,\n" +
                "      \"hl\": \"en\", \"gl\": \"US\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"videoId\": \"$videoId\",\n" +
                "  \"contentCheckOk\": true,\n" +
                "  \"racyCheckOk\": true\n" +
                "}"
        val resp = httpPostJson(
            "https://www.youtube.com/youtubei/v1/player" +
                    "?key=$apiKey&prettyPrint=false&clientVersion=$ANDROID_CLIENT_VERSION",
            body
        ) ?: return null
        return try {
            val root = JSONObject(resp)
            val details = root.optJSONObject("videoDetails")
            details?.optString("title")?.let { if (it.isNotBlank()) metaOut[0] = it }
            details?.optString("author")?.let { if (it.isNotBlank()) metaOut[1] = it }
            val status = root.optJSONObject("playabilityStatus")?.optString("status")
            if (status != null && status != "OK") return null
            parseStreamingData(root)
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------
    // stream parsing / resolving
    // ------------------------------------------------------------------

    private fun parseStreamingData(root: JSONObject): List<RawStream> {
        val out = ArrayList<RawStream>()
        val sd = root.optJSONObject("streamingData") ?: return out
        for (key in listOf("formats", "adaptiveFormats")) {
            val arr = sd.optJSONArray(key) ?: continue
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val mime = o.optString("mimeType").substringBefore(";").trim()
                val hasVideo = mime.startsWith("video")
                val hasAudio = mime.startsWith("audio") || (hasVideo && o.has("audioQuality"))
                val url = o.optString("url").takeIf { it.isNotEmpty() }
                val cipher = o.optString("signatureCipher")
                    .ifEmpty { o.optString("cipher") }
                    .takeIf { it.isNotEmpty() }
                out.add(
                    RawStream(
                        itag = o.optInt("itag"),
                        qualityLabel = o.optString("qualityLabel")
                            .ifEmpty { o.optString("quality") },
                        mime = mime,
                        bitrate = o.optLong("bitrate", 0),
                        length = o.optString("contentLength").toLongOrNull() ?: -1L,
                        url = url,
                        cipher = cipher,
                        hasAudio = hasAudio,
                        hasVideo = hasVideo
                    )
                )
            }
        }
        return out
    }

    private fun resolveStreams(
        raw: List<RawStream>,
        playerJsPath: String?,
        engine: YoutubeJsEngine?
    ): List<Stream> {
        val nRegex = Regex("[?&]n=([^&]+)")
        val needsJs = raw.any { it.cipher != null } ||
                raw.any { it.url != null && nRegex.containsMatchIn(it.url) }

        var jsReady = false
        if (needsJs && engine != null && !playerJsPath.isNullOrEmpty()) {
            val playerUrl = "https://www.youtube.com$playerJsPath"
            val baseJs = httpGet(playerUrl)
            if (baseJs != null) {
                jsReady = engine.ensurePlayer(playerUrl, baseJs)
            }
        }

        val out = ArrayList<Stream>()
        for (r in raw) {
            var url = r.url
            if (url == null) {
                val cipher = r.cipher ?: continue
                if (!jsReady || engine == null) continue
                url = decipher(cipher, engine) ?: continue
            }
            // throttle n-param
            if (jsReady && engine != null) {
                val m = nRegex.find(url)
                if (m != null) {
                    val solved = engine.solveN(m.groupValues[1])
                    if (solved != null) {
                        url = url.replace(
                            "n=${m.groupValues[1]}",
                            "n=${URLEncoder.encode(solved, "UTF-8")}"
                        )
                    }
                }
            }
            out.add(
                Stream(
                    itag = r.itag,
                    qualityLabel = if (r.qualityLabel.isBlank()) labelForItag(r.itag)
                    else r.qualityLabel,
                    container = containerOf(r.mime),
                    bitrate = r.bitrate,
                    contentLength = r.length,
                    url = url,
                    hasAudio = r.hasAudio,
                    hasVideo = r.hasVideo
                )
            )
        }
        return out
    }

    private fun decipher(cipher: String, engine: YoutubeJsEngine): String? {
        var s: String? = null
        var sp: String? = null
        var base: String? = null
        for (pair in cipher.split('&')) {
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val k = pair.substring(0, eq)
            val v = urlDecode(pair.substring(eq + 1))
            when (k) {
                "s" -> s = v
                "sp" -> sp = v
                "url" -> base = v
            }
        }
        val sig = s ?: return null
        val target = base ?: return null
        val solved = engine.solveSignature(sig) ?: return null
        val param = sp ?: "signature"
        return target + "&" + param + "=" + URLEncoder.encode(solved, "UTF-8")
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private fun sortAndDedup(streams: List<Stream>): List<Stream> {
        val seen = HashSet<Int>()
        val uniq = streams.filter { seen.add(it.itag) }
        return uniq.sortedWith { a, b ->
            val gA = groupRank(a)
            val gB = groupRank(b)
            if (gA != gB) gA - gB else (b.bitrate - a.bitrate).toInt()
        }
    }

    private fun groupRank(s: Stream): Int = when {
        s.hasVideo && s.hasAudio -> 0
        s.hasVideo -> 1
        else -> 2
    }

    private fun containerOf(mime: String): String = when {
        mime.contains("mp4") && mime.startsWith("audio") -> "M4A"
        mime.contains("mp4") -> "MP4"
        mime.contains("webm") -> "WEBM"
        mime.contains("x-m4a") -> "M4A"
        else -> mime.substringAfter('/').uppercase().ifBlank { "?" }
    }

    private fun labelForItag(itag: Int): String = when (itag) {
        17 -> "144p"; 18 -> "360p"; 22 -> "720p"
        133 -> "240p"; 134 -> "360p"; 135 -> "480p"; 136 -> "720p"
        137 -> "1080p"; 298 -> "720p60"; 299 -> "1080p60"
        264 -> "1440p"; 271 -> "1440p"; 313 -> "2160p"; 315 -> "2160p60"
        140 -> "audio 128k"; 139 -> "audio 48k"
        249 -> "audio 50k"; 250 -> "audio 70k"; 251 -> "audio 160k"
        else -> "itag $itag"
    }

    private fun firstGroup(text: String, regex: Regex): String? =
        regex.find(text)?.groupValues?.get(1)

    private fun videoTitle(initial: JSONObject?): String? =
        initial?.optJSONObject("videoDetails")?.optString("title")?.takeIf { it.isNotEmpty() }

    private fun videoAuthor(initial: JSONObject?): String? =
        initial?.optJSONObject("videoDetails")?.optString("author")?.takeIf { it.isNotEmpty() }

    private fun extractInitialPlayerResponse(page: String): JSONObject? {
        val marker = "ytInitialPlayerResponse"
        val idx = page.indexOf(marker)
        if (idx < 0) return null
        val start = page.indexOf('{', idx + marker.length)
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        var i = start
        while (i < page.length) {
            val c = page[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            return try {
                                JSONObject(page.substring(start, i + 1))
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
            }
            i++
        }
        return null
    }

    private fun extractPlayerJsPath(page: String): String? {
        firstGroup(page, Regex("\"PLAYER_JS_URL\":\"([^\"]+)\""))?.let {
            return it.replace("\\/", "/")
        }
        val normed = page.replace("\\/", "/")
        return firstGroup(normed, Regex("(/s/player/[A-Za-z0-9_/.\\-]+?\\.js)"))
    }

    private fun urlDecode(s: String): String = try {
        URLDecoder.decode(s, "UTF-8")
    } catch (e: Exception) {
        s
    }

    // ------------------------------------------------------------------
    // http
    // ------------------------------------------------------------------

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", MOBILE_UA)
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            setRequestProperty("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+111")
        }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = openConnection(url)
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { readCapped(it) }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPostJson(url: String, body: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = openConnection(url).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { readCapped(it) }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun readCapped(input: java.io.InputStream): String {
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
        return out.toString(Charsets.UTF_8.name())
    }

    private fun errorResult(videoId: String, message: String): Result =
        Result(videoId = videoId, title = "", author = "", streams = emptyList(), error = message)
}

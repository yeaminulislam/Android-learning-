package com.shieldbrowser.app.yt

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Hidden WebView used purely as a JavaScript runtime to evaluate YouTube's
 * player functions (signature descrambler and the throttling "n" transform).
 * Running the REAL player JS is far more robust than re-implementing the
 * cipher in Kotlin, because YouTube changes the obfuscation frequently.
 *
 * All public methods are blocking with timeouts and safe to call from a
 * background thread (they marshal work onto the main thread internally).
 */
@SuppressLint("SetJavaScriptEnabled")
class YoutubeJsEngine(private val webView: WebView) {

    @Volatile
    private var destroyed = false

    @Volatile
    private var loadedPlayerUrl: String? = null

    @Volatile
    private var ready = false

    @Volatile
    private var sigName: String? = null

    @Volatile
    private var nName: String? = null

    private val main = Handler(Looper.getMainLooper())
    private val loadLock = Any()

    companion object {
        private const val TIMEOUT_LOAD_MS = 25_000L
        private const val TIMEOUT_EVAL_MS = 15_000L

        // fn assignment like: Xy=function(a){a=a.split("")...
        private val SPLIT_FN_ASSIGN =
            Regex("([A-Za-z0-9_\$]+)=function\\(\\w+\\)\\{\\w+=\\w+\\.split\\(\"\"\\)")

        // call-site of the n-transform: .get("n"))&&(b=Xy(b) or b=Nn[0](b)
        private val N_CALL_SITE =
            Regex("\\.get\\(\"n\"\\)\\)&&\\(\\w+=([A-Za-z0-9_\$]{2,})(?:\\[(\\d+)\\])?\\(")

        private fun jsEscape(s: String): String {
            val sb = StringBuilder(s.length + 8)
            for (c in s) {
                when (c) {
                    '\\' -> sb.append("\\\\")
                    '\'' -> sb.append("\\'")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\u2028' -> sb.append("\\u2028")
                    '\u2029' -> sb.append("\\u2029")
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }
    }

    /** Ensures the given player JS is loaded and analysed. Returns true on success. */
    fun ensurePlayer(playerJsUrl: String, baseJs: String): Boolean {
        if (destroyed) return false
        if (ready && loadedPlayerUrl == playerJsUrl) return true
        synchronized(loadLock) {
            if (destroyed) return false
            if (ready && loadedPlayerUrl == playerJsUrl) return true

            analyse(baseJs)
            if (sigName == null && nName == null) return false

            val latch = CountDownLatch(1)
            val html = wrapHtml(baseJs)
            main.post {
                if (destroyed) {
                    latch.countDown()
                    return@post
                }
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        ready = true
                        loadedPlayerUrl = playerJsUrl
                        latch.countDown()
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: android.webkit.WebResourceRequest,
                        error: android.webkit.WebResourceError
                    ) {
                        if (request.isForMainFrame) latch.countDown()
                    }
                }
                webView.settings.javaScriptEnabled = true
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com", html, "text/html", "utf-8", null
                )
            }
            val ok = latch.await(TIMEOUT_LOAD_MS, TimeUnit.MILLISECONDS)
            return ok && ready && loadedPlayerUrl == playerJsUrl
        }
    }

    /** Descrambles a signature. Returns null on failure. */
    fun solveSignature(sig: String): String? {
        val fn = sigName ?: return null
        return evalString("$fn('${jsEscape(sig)}')")
    }

    /** Solves the throttling n-parameter. Returns null on failure. */
    fun solveN(n: String): String? {
        val fn = nName ?: return null
        return evalString("$fn('${jsEscape(n)}')")
    }

    fun destroy() {
        destroyed = true
        ready = false
        main.post {
            try {
                webView.destroy()
            } catch (ignored: Exception) {
            }
        }
    }

    // ----------------------------------------------------------------------

    private fun evalString(jsExpr: String): String? {
        if (destroyed || !ready) return null
        val latch = CountDownLatch(1)
        val holder = arrayOfNulls<String>(1)
        val expr = "(function(){try{return String($jsExpr);}catch(e){return '';}})();"
        main.post {
            if (destroyed) {
                latch.countDown()
                return@post
            }
            try {
                webView.evaluateJavascript(expr) { value ->
                    holder[0] = decodeJsResult(value)
                    latch.countDown()
                }
            } catch (e: Exception) {
                latch.countDown()
            }
        }
        if (!latch.await(TIMEOUT_EVAL_MS, TimeUnit.MILLISECONDS)) return null
        val out = holder[0]
        return out?.takeIf { it.isNotEmpty() }
    }

    private fun decodeJsResult(raw: String?): String? {
        if (raw == null) return null
        return try {
            JSONObject("{\"v\":$raw}").getString("v")
        } catch (e: Exception) {
            raw.trim('"')
        }
    }

    private fun analyse(js: String) {
        val candidates = SPLIT_FN_ASSIGN.findAll(js).map { it.groupValues[1] }.toList()
        sigName = candidates.firstOrNull()

        var n: String? = null
        val callSite = N_CALL_SITE.find(js)
        if (callSite != null) {
            val base = callSite.groupValues[1]
            val indexed = callSite.groupValues.getOrNull(2)
            if (!indexed.isNullOrEmpty()) {
                // indirection: var Xy=[Zn]; -> resolve Zn
                val arr = Regex("var\\s+${Regex.escape(base)}\\s*=\\[([A-Za-z0-9_\$]{2,})\\]")
                    .find(js)
                if (arr != null) n = arr.groupValues[1]
            } else {
                n = base
            }
        }
        if (n == null && candidates.size > 1) {
            // heuristic fallback policy: the n-function is commonly the
            // *other* split-based function (not the signature one)
            n = candidates.firstOrNull { it != sigName }
        }
        nName = n
        if (nName != null && sigName == nName && candidates.size > 1) {
            sigName = candidates.firstOrNull { it != nName }
        }
    }

    private fun wrapHtml(baseJs: String): String {
        // base.js may contain "</script>" inside string literals; escape it so
        // the HTML parser doesn't terminate the script early. "<\/script" is
        // identical JavaScript once evaluated.
        val safe = baseJs.replace("</script", "<\\/script")
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
                "<script>try{${safe}}catch(e){}</script>" +
                "</head><body></body></html>"
    }
}

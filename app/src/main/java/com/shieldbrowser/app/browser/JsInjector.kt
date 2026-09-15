package com.shieldbrowser.app.browser

import android.content.Context
import android.net.Uri
import android.util.LruCache
import android.webkit.WebView
import com.shieldbrowser.app.Prefs
import com.shieldbrowser.app.adblock.AdBlockEngine
import org.json.JSONObject
import java.util.Locale

/** Injects JavaScript/CSS into pages: cosmetic filters, the YouTube ad-skipper,
 *  media event bridge and the background-playback visibility unlocker. */
object JsInjector {

    @Volatile
    private var mediaJs: String? = null

    @Volatile
    private var youtubeJs: String? = null

    @Volatile
    private var cosmoKeeperJs: String? = null

    @Volatile
    private var visibilityJs: String? = null

    private val cosmeticCache = LruCache<String, String>(24)

    fun isYouTubeHost(host: String): Boolean =
        host == "youtube.com" || host == "youtu.be" ||
                host.endsWith(".youtube.com")

    private fun asset(webView: WebView, path: String): String {
        val context: Context = webView.context
        return context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun exec(webView: WebView, js: String) {
        try {
            webView.evaluateJavascript(js, null)
        } catch (ignored: Throwable) {
        }
    }

    /** Full injection on page finished. */
    fun injectAll(webView: WebView, url: String?) {
        if (url == null || !url.startsWith("http")) return
        val host = Uri.parse(url).host?.lowercase(Locale.US) ?: return

        // 1. cosmetic (element hiding)
        if (Prefs.adBlockEnabled && Prefs.cosmeticEnabled && AdBlockEngine.ready) {
            cosmeticJs(webView, host)?.let { exec(webView, it) }
        }

        // 2. YouTube ad skipper
        if (Prefs.youtubeAdSkip && isYouTubeHost(host)) {
            val js = youtubeJs ?: asset(webView, "js/youtube.js").also { youtubeJs = it }
            exec(webView, js)
        }

        // 3. media event bridge (for background play)
        if (Prefs.backgroundPlay) {
            val m = mediaJs ?: asset(webView, "js/mediabridge.js").also { mediaJs = it }
            exec(webView, m)

            // 4. visibility unlocker: pages should think they stay visible
            val v = visibilityJs ?: asset(webView, "js/visibility.js").also { visibilityJs = it }
            exec(webView, v)
        }
    }

    /** Lightweight reinjection for SPA navigations. */
    fun injectLight(webView: WebView, url: String?) {
        if (url == null || !url.startsWith("http")) return
        val host = Uri.parse(url).host?.lowercase(Locale.US) ?: return
        if (Prefs.youtubeAdSkip && isYouTubeHost(host)) {
            val js = youtubeJs ?: asset(webView, "js/youtube.js").also { youtubeJs = it }
            exec(webView, js)
        }
        if (Prefs.adBlockEnabled && Prefs.cosmeticEnabled && AdBlockEngine.ready) {
            cosmeticJs(webView, host)?.let { exec(webView, it) }
        }
    }

    /** Builds (and caches) the cosmetic JS for a host. */
    private fun cosmeticJs(webView: WebView, host: String): String? {
        cosmeticCache.get(host)?.let { cached ->
            return if (cached.isEmpty()) null else cached
        }
        val selectors = AdBlockEngine.cosmeticSelectorsFor(host)
        if (selectors.isEmpty()) {
            cosmeticCache.put(host, "")
            return null
        }
        val css = selectors.joinToString(",") +
                "{display:none!important;visibility:hidden!important;}"
        val quoted = JSONObject.quote(css)
        val keeper = cosmoKeeperJs
            ?: asset(webView, "js/cosmetkeeper.js").also { cosmoKeeperJs = it }
        val js = "(function(){window.__shieldCss=$quoted;$keeper})();"
        cosmeticCache.put(host, js)
        return js
    }
}

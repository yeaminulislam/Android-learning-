package com.shieldbrowser.app.browser

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.shieldbrowser.app.MainActivity
import com.shieldbrowser.app.Prefs
import com.shieldbrowser.app.R
import com.shieldbrowser.app.adblock.AdBlockEngine
import java.io.ByteArrayInputStream

/** WebViewClient with multi-layer ad blocking. */
class BrowserClient(private val tab: BrowserTab, private val host: Host) : WebViewClient() {

    interface Host {
        fun activityOrNull(): MainActivity?
        fun uiPageStarted(tab: BrowserTab, url: String?)
        fun uiPageFinished(tab: BrowserTab, url: String?)
        fun uiUrlChanged(tab: BrowserTab, url: String?)
        fun uiOpenInNewTab(url: String)
        fun uiRenderProcessGone(tab: BrowserTab)
        fun uiSearch(query: String)
        fun uiUserscriptFound(url: String)
    }

    // ---- ad blocking ----------------------------------------------------------------

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (!Prefs.adBlockEnabled || !AdBlockEngine.ready) return null
        val url = request.url?.toString() ?: return null
        if (request.isForMainFrame) {
            // Deliberate navigation (typed URL / real tap) is never blocked.
            if (request.hasGesture()) return null
            if (AdBlockEngine.isBlocked(view.url, url, true)) {
                AdBlockEngine.onBlocked()
                Telemetry.logToActivity(host, url)
                return blockedPage(url)
            }
            return null
        }
        return if (AdBlockEngine.isBlocked(view.url, url, false)) {
            AdBlockEngine.onBlocked()
            emptyResponse()
        } else {
            null
        }
    }

    private fun emptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

    private fun blockedPage(blockedUrl: String): WebResourceResponse {
        val safeName = Uri.parse(blockedUrl).host ?: blockedUrl
        val html = "<html><head><meta name=\"viewport\" content=\"width=device-width," +
                "initial-scale=1\">" +
                "<style>body{font-family:sans-serif;background:#0d1117;color:#e6edf3;" +
                "display:flex;flex-direction:column;align-items:center;justify-content:center;" +
                "height:100vh;margin:0;text-align:center;padding:24px}</style></head><body>" +
                "<div style=\"font-size:64px\">🛡️</div>" +
                "<h2>Blocked by Shield Browser</h2>" +
                "<p style=\"color:#8b949e\">$safeName</p>" +
                "<p style=\"color:#8b949e\">This page is a known ad/tracker.</p>" +
                "</body></html>"
        val data = ByteArrayInputStream(html.toByteArray(Charsets.UTF_8))
        return WebResourceResponse(
            "text/html", "utf-8", 403, "Blocked by Shield",
            emptyMap<String, String>(), data
        )
    }

    // ---- navigation -----------------------------------------------------------------

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url?.toString() ?: return false
        val scheme = request.url?.scheme?.lowercase() ?: return false
        // internal pseudo-host used by the bundled home page
        if ((scheme == "https" || scheme == "http") &&
            request.url?.host == "shield.local"
        ) {
            val query = request.url?.getQueryParameter("q").orEmpty()
            host.uiSearch(query)
            return true
        }
        // userscript install: *.user.js links open the installer instead
        if ((scheme == "https" || scheme == "http") &&
            com.shieldbrowser.app.ext.Userscript.looksLikeUserscriptUrl(url)
        ) {
            host.uiUserscriptFound(url)
            return true
        }
        return when (scheme) {
            "http", "https", "about", "data", "file", "javascript" -> false
            "intent" -> {
                handleIntentScheme(view, url)
                true
            }
            else -> {
                openExternal(Uri.parse(url))
                true
            }
        }
    }

    private fun handleIntentScheme(view: WebView, url: String) {
        val activity = host.activityOrNull() ?: return
        try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            try {
                activity.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // fall through to fallback / Play Store
            }
            val fallback = intent.getStringExtra("browser_fallback_url")
            if (!fallback.isNullOrEmpty()) {
                view.loadUrl(fallback)
                return
            }
            val pkg = intent.`package`
            if (!pkg.isNullOrEmpty()) {
                openExternal(Uri.parse("market://details?id=$pkg"))
            }
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.cant_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternal(uri: Uri) {
        val activity = host.activityOrNull() ?: return
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.cant_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    // ---- page lifecycle -------------------------------------------------------------

    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
        AdBlockEngine.onNewPage()
        // document-start userscripts (as early as WebView allows)
        com.shieldbrowser.app.ext.UserscriptEngine.inject(
            view, url, com.shieldbrowser.app.ext.Userscript.RunAt.START
        )
        host.uiPageStarted(tab, url)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        // inject cosmetic filters + helper scripts
        JsInjector.injectAll(view, url)
        // document-end userscripts
        com.shieldbrowser.app.ext.UserscriptEngine.inject(
            view, url, com.shieldbrowser.app.ext.Userscript.RunAt.END
        )
        host.uiPageFinished(tab, url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        // single-page apps (YouTube) change URL without full loads: re-inject
        if (!isReload) JsInjector.injectLight(view, url)
        host.uiUrlChanged(tab, url)
    }

    // ---- errors & security ------------------------------------------------------------

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError
    ) {
        if (request.isForMainFrame) {
            val msg = error.description?.toString() ?: "Error"
            val html = "<html><head><meta name=\"viewport\" content=\"width=device-width," +
                    "initial-scale=1\"><style>body{font-family:sans-serif;background:#0d1117;" +
                    "color:#e6edf3;display:flex;flex-direction:column;align-items:center;" +
                    "justify-content:center;height:100vh;margin:0;text-align:center;padding:24px}" +
                    "</style></head><body><div style=\"font-size:64px\">🔌</div>" +
                    "<h2>Page could not be loaded</h2>" +
                    "<p style=\"color:#8b949e\">$msg</p></body></html>"
            view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val activity = host.activityOrNull()
        if (activity == null) {
            handler.cancel()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.ssl_error_title)
            .setMessage(activity.getString(R.string.ssl_error_msg))
            .setPositiveButton(R.string.ssl_continue) { _, _ -> handler.proceed() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> handler.cancel() }
            .show()
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: android.webkit.RenderProcessGoneDetail
    ): Boolean {
        host.uiRenderProcessGone(tab)
        return true
    }
}

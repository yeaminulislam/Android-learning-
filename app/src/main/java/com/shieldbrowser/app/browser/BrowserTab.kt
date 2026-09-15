package com.shieldbrowser.app.browser

import android.view.ViewGroup
import android.webkit.WebView
import com.shieldbrowser.app.BrowserApp
import com.shieldbrowser.app.MainActivity
import java.util.UUID

/** One browser tab = one WebView plus its UI state. */
class BrowserTab(val activity: MainActivity, initialUrl: String? = null) {

    val id: String = UUID.randomUUID().toString()

    var webView: WebView = WebViewFactory.create(activity, this)
        private set

    var title: String = ""

    val url: String?
        get() = webView.url

    init {
        webView.loadUrl(initialUrl ?: BrowserApp.HOME_URL)
    }

    /** Recreate the WebView, e.g. after the render process crashed. */
    fun recreate() {
        val current = webView.url
        val old = webView
        (old.parent as? ViewGroup)?.removeView(old)
        try {
            old.destroy()
        } catch (ignored: Exception) {
        }
        webView = WebViewFactory.create(activity, this)
        webView.loadUrl(current ?: BrowserApp.HOME_URL)
    }

    fun destroy() {
        MediaController.detach(this)
        (webView.parent as? ViewGroup)?.removeView(webView)
        try {
            webView.destroy()
        } catch (ignored: Exception) {
        }
    }
}

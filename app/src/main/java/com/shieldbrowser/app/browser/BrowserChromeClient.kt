package com.shieldbrowser.app.browser

import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.shieldbrowser.app.Prefs
import com.shieldbrowser.app.adblock.AdBlockEngine

/** WebChromeClient: progress, titles, pop-up policy, file chooser, fullscreen video. */
class BrowserChromeClient(private val tab: BrowserTab, private val host: Host) : WebChromeClient() {

    interface Host {
        fun uiProgress(progress: Int)
        fun uiTitle(tab: BrowserTab, title: String?)
        fun uiShowFileChooser(callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean
        fun uiEnterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback)
        fun uiExitFullscreen()
        fun uiPopupBlocked()
        fun uiOpenInNewTab(url: String)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        host.uiProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        tab.title = title ?: ""
        host.uiTitle(tab, title)
    }

    /**
     * window.open / target=_blank handling.
     * Without a user gesture it's a classic ad pop-up → blocked & counted.
     * With a user gesture we open it as a real tab.
     */
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        if (Prefs.blockPopups && !isUserGesture) {
            AdBlockEngine.onPopupBlocked()
            host.uiPopupBlocked()
            return false
        }
        val temp = WebView(view.context)
        temp.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldOverrideUrlLoading(
                v: WebView,
                request: android.webkit.WebResourceRequest
            ): Boolean {
                passOut(v, request.url?.toString())
                return true
            }

            override fun onPageStarted(v: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                passOut(v, url)
            }

            private fun passOut(v: WebView, url: String?) {
                try {
                    (v.parent as? android.view.ViewGroup)?.removeView(v)
                    v.destroy()
                } catch (ignored: Exception) {
                }
                if (!url.isNullOrEmpty()) host.uiOpenInNewTab(url)
            }
        }
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = temp
        resultMsg.sendToTarget()
        return true
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean = host.uiShowFileChooser(filePathCallback, fileChooserParams)

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        host.uiEnterFullscreen(view, callback)
    }

    override fun onHideCustomView() {
        host.uiExitFullscreen()
    }
}

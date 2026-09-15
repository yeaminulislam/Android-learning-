package com.shieldbrowser.app.browser

import android.webkit.JavascriptInterface

/** Bridge called from injected JavaScript whenever a <video>/<audio> element
 *  starts or stops playing, so the app can keep background playback alive. */
class MediaBridge(private val tab: BrowserTab) {

    @JavascriptInterface
    fun onMediaPlay() {
        MediaController.onEvent(tab, MediaController.EVENT_PLAY)
    }

    @JavascriptInterface
    fun onMediaPause() {
        MediaController.onEvent(tab, MediaController.EVENT_PAUSE)
    }

    @JavascriptInterface
    fun onMediaEnded() {
        MediaController.onEvent(tab, MediaController.EVENT_ENDED)
    }
}

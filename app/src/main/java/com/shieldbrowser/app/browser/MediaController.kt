package com.shieldbrowser.app.browser

import android.os.Handler
import android.os.Looper
import com.shieldbrowser.app.MainActivity
import com.shieldbrowser.app.PlaybackService
import com.shieldbrowser.app.Prefs
import java.lang.ref.WeakReference

/** Tracks which tab currently owns media playback and talks to the
 *  foreground [PlaybackService] that keeps audio alive in the background. */
object MediaController {

    const val EVENT_PLAY = 1
    const val EVENT_PAUSE = 2
    const val EVENT_ENDED = 3

    const val CMD_PLAY = 1
    const val CMD_PAUSE = 2

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var activeTab: WeakReference<BrowserTab>? = null

    @Volatile
    var hostActivity: WeakReference<MainActivity>? = null

    fun activeTab(): BrowserTab? = activeTab?.get()

    /** Called from the [MediaBridge] (JS side) – may be on any thread. */
    fun onEvent(tab: BrowserTab, event: Int) {
        when (event) {
            EVENT_PLAY -> {
                if (!Prefs.backgroundPlay) return
                val activity = hostActivity?.get() ?: return
                main.post { attachPlay(tab, activity) }
            }
            EVENT_ENDED -> {
                val current = activeTab?.get() ?: return
                if (current !== tab) return
                val activity = hostActivity?.get() ?: return
                activeTab = null
                PlaybackService.sendAction(activity, PlaybackService.ACTION_STOP)
            }
            else -> Unit
        }
    }

    private fun attachPlay(tab: BrowserTab, activity: MainActivity) {
        val prev = activeTab?.get()
        if (prev != null && prev !== tab) {
            // a different tab was playing: pause it first
            runJs(prev.webView, "pause")
        }
        activeTab = WeakReference(tab)
        val title = tab.title.ifBlank { tab.webView.url ?: "Media" }
        PlaybackService.start(activity, title)
    }

    /** Command coming from the media notification / session (play or pause). */
    fun command(cmd: Int) {
        val tab = activeTab?.get() ?: return
        main.post {
            try {
                runJs(tab.webView, if (cmd == CMD_PLAY) "play" else "pause")
            } catch (ignored: Exception) {
            }
        }
    }

    fun detach(tab: BrowserTab) {
        if (activeTab?.get() === tab) activeTab = null
    }

    private fun runJs(webView: android.webkit.WebView, op: String) {
        try {
            webView.evaluateJavascript(
                "(function(){try{var m=document.querySelectorAll('video,audio');" +
                        "for(var i=0;i<m.length;i++){try{m[i].$op();}catch(e){}}}catch(e){}})();",
                null
            )
        } catch (ignored: Throwable) {
        }
    }
}

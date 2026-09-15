package com.shieldbrowser.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.shieldbrowser.app.adblock.AdBlockEngine
import com.shieldbrowser.app.ext.UserscriptEngine
import com.shieldbrowser.app.store.BookmarkStore
import com.shieldbrowser.app.store.HistoryStore

class BrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        createNotificationChannel()

        // Heavy work off the main thread.
        Thread {
            BookmarkStore.warmUp(this)
            HistoryStore.warmUp(this)
        }.start()

        // Load the ad-block engine (filter lists) in the background.
        AdBlockEngine.warmUp(this)

        // Load installed userscripts (extensions) in the background.
        UserscriptEngine.warmUp(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    PlaybackService.CHANNEL_ID,
                    getString(R.string.channel_playback),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    setSound(null, null)
                }
            )
        }
    }

    companion object {
        const val HOME_URL = "file:///android_asset/home.html"
    }
}

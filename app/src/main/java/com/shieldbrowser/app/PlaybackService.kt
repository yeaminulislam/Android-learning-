package com.shieldbrowser.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.shieldbrowser.app.browser.MediaController

/**
 * Foreground service that keeps audio playing when the app goes to the
 * background (YouTube & any other media site). Shows a media notification
 * with play/pause/stop controls backed by a MediaSession.
 */
class PlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "shield_playback"
        private const val NOTIF_ID = 1001

        const val ACTION_START = "com.shieldbrowser.app.ACTION_PLAYBACK_START"
        const val ACTION_PLAY = "com.shieldbrowser.app.ACTION_PLAYBACK_PLAY"
        const val ACTION_PAUSE = "com.shieldbrowser.app.ACTION_PLAYBACK_PAUSE"
        const val ACTION_STOP = "com.shieldbrowser.app.ACTION_PLAYBACK_STOP"
        private const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, title: String) {
            val intent = Intent(context, PlaybackService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TITLE, title)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (ignored: Exception) {
            }
        }

        fun sendAction(context: Context, action: String) {
            try {
                context.startService(Intent(context, PlaybackService::class.java).setAction(action))
            } catch (ignored: Exception) {
            }
        }
    }

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var title: String = ""
    private var playing = true
    private var foreground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        title = getString(R.string.notif_playing)
        mediaSession = MediaSession(this, "ShieldBrowser").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    MediaController.command(MediaController.CMD_PLAY)
                    setPlaying(true)
                }

                override fun onPause() {
                    MediaController.command(MediaController.CMD_PAUSE)
                    setPlaying(false)
                }

                override fun onStop() {
                    internalStop()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() }?.let { title = it }
                setPlaying(true)
                acquireWakeLock()
                if (!foreground) {
                    startForegroundNow()
                    foreground = true
                } else {
                    refreshNotification()
                }
                updateSession()
            }
            ACTION_PLAY -> {
                MediaController.command(MediaController.CMD_PLAY)
                setPlaying(true)
            }
            ACTION_PAUSE -> {
                MediaController.command(MediaController.CMD_PAUSE)
                setPlaying(false)
            }
            ACTION_STOP -> {
                MediaController.command(MediaController.CMD_PAUSE)
                internalStop()
            }
        }
        return START_STICKY
    }

    private fun setPlaying(value: Boolean) {
        playing = value
        updateSession()
        refreshNotification()
    }

    // ---- foreground / notification -------------------------------------------------

    private fun startForegroundNow() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun refreshNotification() {
        if (!foreground) return
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIF_ID, buildNotification())
    }

    private fun servicePending(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseAction: Notification.Action = if (playing) {
            Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                getString(R.string.action_pause),
                servicePending(ACTION_PAUSE)
            ).build()
        } else {
            Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_media_play),
                getString(R.string.action_play),
                servicePending(ACTION_PLAY)
            ).build()
        }
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_delete),
            getString(R.string.action_stop),
            servicePending(ACTION_STOP)
        ).build()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        builder
            .setContentTitle(title)
            .setContentText(getString(R.string.notif_tap_return))
            .setSmallIcon(R.drawable.ic_shield_notif)
            .setContentIntent(openIntent)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .addAction(stopAction)

        mediaSession?.let {
            builder.setStyle(
                Notification.MediaStyle()
                    .setMediaSession(it.sessionToken)
                    .setShowActionsInCompactView(0)
            )
        }
        return builder.build()
    }

    private fun updateSession() {
        val session = mediaSession ?: return
        val state = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_PLAY_PAUSE
            )
            .setState(
                if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f
            )
            .build()
        session.setPlaybackState(state)
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, getString(R.string.app_name))
                .build()
        )
    }

    // ---- wake lock ------------------------------------------------------------------

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "shieldbrowser:background-playback"
            ).apply { setReferenceCounted(false) }
        }
        try {
            wakeLock?.takeIf { !it.isHeld }?.acquire()
        } catch (ignored: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (ignored: Exception) {
        }
    }

    // ---- shutdown -------------------------------------------------------------------

    private fun internalStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foreground = false
        releaseWakeLock()
        mediaSession?.isActive = false
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLock()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}

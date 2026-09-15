package com.shieldbrowser.app

import androidx.appcompat.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Global crash logger.
 *
 * Any uncaught exception on ANY thread is written to `files/last-crash.txt`
 * before the process dies. On the next launch, MainActivity shows the log in
 * a dialog (once per crash) so the user can copy it and share it. Guarded
 * background paths also [record] their non-fatal failures here so silent
 * errors become visible too.
 */
object CrashGuard {

    private const val TAG = "CrashGuard"
    private const val FILE_NAME = "last-crash.txt"
    private const val SHOWN_MARK = "last-crash-shown.txt"
    private const val MAX_FILE_CHARS = 48 * 1024
    private const val DIALOG_CHARS = 5000

    @Volatile
    private var filesBase: File? = null

    private val installed = AtomicBoolean(false)

    /** Installs the global handler. Must be the very first call in Application.onCreate. */
    fun install(context: Context) {
        filesBase = context.applicationContext.filesDir
        if (installed.getAndSet(true)) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                record("FATAL on thread '${thread.name}'", throwable)
            } catch (ignored: Throwable) {
            }
            try {
                if (previous != null) {
                    previous.uncaughtException(thread, throwable)
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            } catch (ignored: Throwable) {
            }
        }
    }

    /** Records a failure (fatal or non-fatal) into the crash file, newest first. */
    @Synchronized
    fun record(where: String, t: Throwable) {
        try {
            Log.e(TAG, where, t)
            val dir = filesBase ?: return
            val entry = buildString {
                append(stamp()).append("  [").append(where).append("]\n")
                val sw = StringWriter()
                t.printStackTrace(PrintWriter(sw))
                append(sw.toString()).append('\n')
            }
            val f = File(dir, FILE_NAME)
            val old = if (f.exists() && f.length() < MAX_FILE_CHARS * 4L) {
                try {
                    f.readText()
                } catch (e: Throwable) {
                    ""
                }
            } else ""
            var text = entry + "\n" + old
            if (text.length > MAX_FILE_CHARS) text = text.take(MAX_FILE_CHARS)
            f.writeText(text)
        } catch (ignored: Throwable) {
        }
    }

    /** Shows the pending crash log (once per unique crash) in the given activity. */
    fun maybeShowPending(activity: FragmentActivity) {
        try {
            val f = File(activity.filesDir, FILE_NAME)
            if (!f.exists() || f.length() == 0L) return
            val content = try {
                f.readText().trim()
            } catch (t: Throwable) {
                return
            }
            if (content.isEmpty()) return

            val marker = File(activity.filesDir, SHOWN_MARK)
            val hash = content.hashCode().toString()
            val alreadyShown = try {
                marker.exists() && marker.readText() == hash
            } catch (t: Throwable) {
                false
            }
            if (alreadyShown) return

            val header = header(activity)
            val shown = (header + content).take(DIALOG_CHARS)
            val textView = TextView(activity).apply {
                text = shown
                textSize = 11f
                typeface = Typeface.MONOSPACE
                setTextIsSelectable(true)
            }
            val pad = (14 * activity.resources.displayMetrics.density).toInt()
            val scroll = ScrollView(activity).apply {
                addView(textView)
                setPadding(pad, pad / 2, pad, 0)
            }

            fun markShown() {
                try {
                    marker.writeText(hash)
                } catch (ignored: Throwable) {
                }
            }

            AlertDialog.Builder(activity)
                .setTitle(R.string.crash_title)
                .setMessage(R.string.crash_msg)
                .setView(scroll)
                .setPositiveButton(R.string.crash_copy) { _, _ ->
                    copyToClipboard(activity, header + content)
                    markShown()
                    Toast.makeText(activity, R.string.crash_copied, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.crash_dismiss) { _, _ -> markShown() }
                .setCancelable(false)
                .show()
        } catch (ignored: Throwable) {
        }
    }

    private fun header(context: Context): String = buildString {
        append("Shield Browser ").append(BuildConfig.VERSION_NAME)
            .append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
        append("Android ").append(Build.VERSION.RELEASE)
            .append(" (API ").append(Build.VERSION.SDK_INT).append(") · ")
            .append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
        append('\n').append(stamp()).append("\n\n")
    }

    private fun copyToClipboard(context: Context, text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("crash-log", text))
        } catch (ignored: Throwable) {
        }
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}

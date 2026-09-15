package com.shieldbrowser.app.browser

/** Tiny hook so the client can nudge the UI when a main-frame page gets blocked. */
object Telemetry {
    fun logToActivity(host: BrowserClient.Host, blockedUrl: String) {
        // currently just logs; keeping indirection avoids hard wiring UI here
        android.util.Log.i("ShieldBrowser", "Blocked main frame: $blockedUrl")
    }
}

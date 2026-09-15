package com.shieldbrowser.app

import android.content.Context
import android.content.SharedPreferences

/** Simple wrapper around SharedPreferences for all app settings. */
object Prefs {
    private const val FILE = "shield_prefs"

    private const val KEY_ADBLOCK = "adblock_enabled"
    private const val KEY_COSMETIC = "cosmetic_enabled"
    private const val KEY_POPUPS = "popups_blocked"
    private const val KEY_YT_SKIP = "youtube_skip"
    private const val KEY_BACKGROUND = "background_play"
    private const val KEY_DESKTOP = "desktop_mode"
    private const val KEY_SEARCH = "search_engine"
    private const val KEY_BLOCKED_TOTAL = "blocked_total"

    lateinit var sp: SharedPreferences
        private set

    fun init(context: Context) {
        sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var adBlockEnabled: Boolean
        get() = sp.getBoolean(KEY_ADBLOCK, true)
        set(v) = sp.edit().putBoolean(KEY_ADBLOCK, v).apply()

    var cosmeticEnabled: Boolean
        get() = sp.getBoolean(KEY_COSMETIC, true)
        set(v) = sp.edit().putBoolean(KEY_COSMETIC, v).apply()

    var blockPopups: Boolean
        get() = sp.getBoolean(KEY_POPUPS, true)
        set(v) = sp.edit().putBoolean(KEY_POPUPS, v).apply()

    var youtubeAdSkip: Boolean
        get() = sp.getBoolean(KEY_YT_SKIP, true)
        set(v) = sp.edit().putBoolean(KEY_YT_SKIP, v).apply()

    var backgroundPlay: Boolean
        get() = sp.getBoolean(KEY_BACKGROUND, true)
        set(v) = sp.edit().putBoolean(KEY_BACKGROUND, v).apply()

    var desktopMode: Boolean
        get() = sp.getBoolean(KEY_DESKTOP, false)
        set(v) = sp.edit().putBoolean(KEY_DESKTOP, v).apply()

    var searchEngine: String
        get() = sp.getString(KEY_SEARCH, "google") ?: "google"
        set(v) = sp.edit().putString(KEY_SEARCH, v).apply()

    var blockedTotal: Long
        get() = sp.getLong(KEY_BLOCKED_TOTAL, 0L)
        set(v) = sp.edit().putLong(KEY_BLOCKED_TOTAL, v).apply()
}

package com.shieldbrowser.app.store

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** JSON-file backed history storage (newest first, capped). */
object HistoryStore {

    data class Entry(val title: String, val url: String, val time: Long)

    private const val MAX_ENTRIES = 500

    private val items = ArrayList<Entry>()
    private val lock = Any()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var loaded = false

    fun warmUp(context: Context) {
        appContext = context.applicationContext
        synchronized(lock) {
            if (loaded) return
            loadFromDisk()
            loaded = true
        }
    }

    private fun ensureLoaded() {
        val ctx = appContext ?: return
        warmUp(ctx)
    }

    private fun file(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, "history.json")
    }

    private fun loadFromDisk() {
        try {
            val f = file() ?: return
            if (!f.exists()) return
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                items.add(Entry(o.optString("t"), o.optString("u"), o.optLong("time")))
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun saveToDisk() {
        val f = file() ?: return
        val snapshot: List<Entry> = synchronized(lock) { ArrayList(items) }
        Thread {
            try {
                val arr = JSONArray()
                for (e in snapshot) {
                    arr.put(
                        JSONObject()
                            .put("t", e.title)
                            .put("u", e.url)
                            .put("time", e.time)
                    )
                }
                f.writeText(arr.toString())
            } catch (ignored: Throwable) {
            }
        }.start()
    }

    fun all(): List<Entry> {
        ensureLoaded()
        return synchronized(lock) { ArrayList(items) }
    }

    fun add(title: String?, url: String) {
        if (url.isBlank() || !url.startsWith("http")) return
        ensureLoaded()
        synchronized(lock) {
            if (items.isNotEmpty() && items[0].url == url) {
                // refresh title of consecutive duplicate
                items[0] = Entry(title ?: items[0].title, url, System.currentTimeMillis())
            } else {
                items.add(0, Entry(title ?: url, url, System.currentTimeMillis()))
                while (items.size > MAX_ENTRIES) items.removeAt(items.size - 1)
            }
        }
        saveToDisk()
    }

    fun removeAt(index: Int) {
        ensureLoaded()
        synchronized(lock) { if (index in items.indices) items.removeAt(index) }
        saveToDisk()
    }

    fun clear() {
        ensureLoaded()
        synchronized(lock) { items.clear() }
        saveToDisk()
    }
}

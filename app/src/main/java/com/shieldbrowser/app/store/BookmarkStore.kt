package com.shieldbrowser.app.store

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** JSON-file backed bookmark storage. */
object BookmarkStore {

    data class Bookmark(val title: String, val url: String)

    private val items = ArrayList<Bookmark>()
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
        return File(ctx.filesDir, "bookmarks.json")
    }

    private fun loadFromDisk() {
        try {
            val f = file() ?: return
            if (!f.exists()) return
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                items.add(Bookmark(o.optString("t"), o.optString("u")))
            }
        } catch (ignored: Throwable) {
        }
    }

    private fun saveToDisk() {
        val f = file() ?: return
        val snapshot: List<Bookmark> = synchronized(lock) { ArrayList(items) }
        Thread {
            try {
                val arr = JSONArray()
                for (b in snapshot) {
                    arr.put(JSONObject().put("t", b.title).put("u", b.url))
                }
                f.writeText(arr.toString())
            } catch (ignored: Throwable) {
            }
        }.start()
    }

    fun all(): List<Bookmark> {
        ensureLoaded()
        return synchronized(lock) { ArrayList(items) }
    }

    fun contains(url: String?): Boolean {
        if (url == null) return false
        ensureLoaded()
        return synchronized(lock) { items.any { it.url == url } }
    }

    /** Returns true when added, false when removed. */
    fun toggle(title: String, url: String): Boolean {
        ensureLoaded()
        return if (contains(url)) {
            remove(url)
            false
        } else {
            add(title, url)
            true
        }
    }

    fun add(title: String, url: String) {
        ensureLoaded()
        synchronized(lock) { items.add(Bookmark(title, url)) }
        saveToDisk()
    }

    fun remove(url: String) {
        ensureLoaded()
        synchronized(lock) { items.removeAll { it.url == url } }
        saveToDisk()
    }

    fun removeAt(index: Int) {
        ensureLoaded()
        synchronized(lock) { if (index in items.indices) items.removeAt(index) }
        saveToDisk()
    }
}

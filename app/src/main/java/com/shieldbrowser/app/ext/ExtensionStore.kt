package com.shieldbrowser.app.ext

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

/** Persistent storage for installed userscripts and their GM values. */
object ExtensionStore {

    private const val DIR = "extensions"
    private const val REQ_DIR = "requires"

    @Volatile
    private var appContext: Context? = null

    private fun dir(): File {
        val ctx = appContext ?: throw IllegalStateException("ExtensionStore not initialised")
        return File(ctx.filesDir, DIR).apply { if (!exists()) mkdirs() }
    }

    private fun reqDir(): File =
        File(dir(), REQ_DIR).apply { if (!exists()) mkdirs() }

    fun init(context: Context) {
        appContext = context.applicationContext
        dir()
    }

    // ---------------- CRUD ----------------

    fun all(): List<Userscript> {
        val out = ArrayList<Userscript>()
        val d = try {
            dir()
        } catch (e: Exception) {
            return out
        }
        d.listFiles { f -> f.name.endsWith(".json") }?.forEach { meta ->
            try {
                val js = JSONObject(meta.readText())
                val id = js.getString("id")
                val codeFile = File(d, "$id.user.js")
                if (!codeFile.exists()) return@forEach
                val script = Userscript.parse(
                    id = id,
                    source = codeFile.readText(),
                    enabled = js.optBoolean("enabled", true)
                ) ?: return@forEach
                out.add(script)
            } catch (ignored: Exception) {
            }
        }
        return out.sortedBy { it.name.lowercase() }
    }

    /** Installs a script; if the same namespace+name exists it is REPLACED (update). */
    fun install(source: String): Userscript? {
        val id = UUID.randomUUID().toString()
        val parsed = Userscript.parse(id, source) ?: return null

        // update path: same namespace+name already installed?
        if (parsed.namespace.isNotEmpty()) {
            for (existing in all()) {
                if (existing.namespace == parsed.namespace && existing.name == parsed.name) {
                    return replace(existing.id, source)
                }
            }
        }

        val enabled = true
        writeMeta(parsed, enabled)
        File(dir(), "$id.user.js").writeText(source)
        fetchRequires(id, parsed.requires)
        return parsed
    }

    private fun replace(id: String, source: String): Userscript? {
        val parsed = Userscript.parse(id, source) ?: return null
        val old = File(dir(), "$id.json")
        val wasEnabled = try {
            JSONObject(old.readText()).optBoolean("enabled", true)
        } catch (e: Exception) {
            true
        }
        File(dir(), "$id.user.js").writeText(source)
        writeMeta(parsed, wasEnabled)
        fetchRequires(id, parsed.requires)
        return parsed
    }

    private fun writeMeta(script: Userscript, enabled: Boolean) {
        val meta = JSONObject()
            .put("id", script.id)
            .put("enabled", enabled)
            .put("installedAt", System.currentTimeMillis())
        File(dir(), "${script.id}.json").writeText(meta.toString())
    }

    fun setEnabled(id: String, enabled: Boolean) {
        try {
            val f = File(dir(), "$id.json")
            val js = JSONObject(f.readText())
            js.put("enabled", enabled)
            f.writeText(js.toString())
        } catch (ignored: Exception) {
        }
    }

    fun remove(id: String) {
        File(dir(), "$id.json").delete()
        File(dir(), "$id.user.js").delete()
        File(valuesDir(), "$id.json").delete()
    }

    // ---------------- @require cache ----------------

    private fun sha1(text: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun fetchRequires(ownerId: String, urls: List<String>) {
        if (urls.isEmpty()) return
        Thread {
            for (url in urls) {
                try {
                    val f = File(reqDir(), sha1(url) + ".js")
                    if (f.exists() && f.length() > 0) continue
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        setRequestProperty("User-Agent", "ShieldBrowser/1.1")
                    }
                    if (conn.responseCode in 200..299) {
                        f.writeText(conn.inputStream.bufferedReader().use { it.readText() })
                    }
                    conn.disconnect()
                } catch (ignored: Exception) {
                }
            }
        }.start()
    }

    fun requiredSource(url: String): String? {
        val f = File(reqDir(), sha1(url) + ".js")
        return try {
            if (f.exists() && f.length() > 0) f.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- GM values ----------------

    private val valuesLock = Any()

    private fun valuesDir(): File =
        File(dir(), "values").apply { if (!exists()) mkdirs() }

    fun valuesJson(scriptId: String): String {
        return try {
            val f = File(valuesDir(), "$scriptId.json")
            if (f.exists()) f.readText() else "{}"
        } catch (e: Exception) {
            "{}"
        }
    }

    fun putValue(scriptId: String, key: String, jsonValue: String) {
        synchronized(valuesLock) {
            try {
                val f = File(valuesDir(), "$scriptId.json")
                val js = try {
                    if (f.exists()) JSONObject(f.readText()) else JSONObject()
                } catch (e: Exception) {
                    JSONObject()
                }
                js.put(key, parseJsonValue(jsonValue))
                f.writeText(js.toString())
            } catch (ignored: Exception) {
            }
        }
    }

    fun deleteValue(scriptId: String, key: String) {
        synchronized(valuesLock) {
            try {
                val f = File(valuesDir(), "$scriptId.json")
                if (!f.exists()) return
                val js = JSONObject(f.readText())
                js.remove(key)
                f.writeText(js.toString())
            } catch (ignored: Exception) {
            }
        }
    }

    private fun parseJsonValue(raw: String): Any {
        // The bridge sends JSON-encoded values; store them re-parsed.
        return try {
            org.json.JSONTokener(raw).nextValue()
        } catch (e: Exception) {
            raw
        }
    }
}

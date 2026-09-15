package com.shieldbrowser.app.ext

/** One installed userscript (.user.js): parsed metadata + full source. */
class Userscript(
    val id: String,
    val name: String,
    val namespace: String,
    val version: String,
    val description: String,
    val runAt: RunAt,
    val includes: List<String>,
    val excludes: List<String>,
    val requires: List<String>,
    val homepage: String,
    val source: String,
    var enabled: Boolean
) {
    enum class RunAt { START, END }

    fun matches(url: String): Boolean {
        if (includes.isEmpty()) return false
        for (ex in excludes) {
            if (MatchPattern.urlMatches(ex, url)) return false
        }
        for (inc in includes) {
            if (inc == "<all_urls>") return true
            if (MatchPattern.urlMatches(inc, url)) return true
        }
        return false
    }

    companion object {
        /** True when [url] looks like a downloadable userscript. */
        fun looksLikeUserscriptUrl(url: String): Boolean {
            val q = url.indexOf('?')
            val path = if (q >= 0) url.substring(0, q) else url
            return path.endsWith(".user.js") || path.endsWith(".userjs")
        }

        /** Parses the ==UserScript== metadata block. */
        fun parse(id: String, source: String, enabled: Boolean = true): Userscript? {
            val start = source.indexOf("// ==UserScript==")
            val end = source.indexOf("// ==/UserScript==")
            if (start < 0 || end <= start) return null
            val block = source.substring(start, end)

            var name = ""
            var namespace = ""
            var version = "1.0"
            var description = ""
            var homepage = ""
            var runAt = RunAt.END
            val includes = ArrayList<String>()
            val excludes = ArrayList<String>()
            val requires = ArrayList<String>()

            for (rawLine in block.lines()) {
                val line = rawLine.trim()
                if (!line.startsWith("//")) continue
                val body = line.removePrefix("//").trim()
                if (!body.startsWith("@")) continue
                val space = body.indexOf(' ')
                if (space < 0) continue
                val key = body.substring(1, space).trim()
                val value = body.substring(space + 1).trim()
                when (key) {
                    "name" -> name = value
                    "namespace" -> namespace = value
                    "version" -> version = value
                    "description" -> description = value
                    "homepage", "homepageURL", "website", "source" -> homepage = value
                    "match", "include" -> includes.add(value)
                    "exclude", "exclude-match" -> excludes.add(value)
                    "require" -> requires.add(value)
                    "run-at" -> runAt = when (value) {
                        "document-start" -> RunAt.START
                        else -> RunAt.END   // document-end & document-idle both map to END
                    }
                }
            }
            if (name.isEmpty()) name = "userscript-$id"
            if (includes.isEmpty()) includes.add("*://*/*")   // default: everywhere
            return Userscript(
                id = id,
                name = name,
                namespace = namespace,
                version = version,
                description = description,
                runAt = runAt,
                includes = includes,
                excludes = excludes,
                requires = requires,
                homepage = homepage,
                source = source,
                enabled = enabled
            )
        }
    }
}

package com.shieldbrowser.app.ext

/**
 * Chrome/Firefox-style match patterns (e.g. `*://*.example.com/path`) and
 * Greasemonkey globs (e.g. `*.google.com/path`) converted to cached regexes.
 */
object MatchPattern {

    private val cache = HashMap<String, Regex?>()

    @Synchronized
    fun urlMatches(pattern: String, url: String): Boolean {
        val regex = cache.getOrPut(pattern) { compile(pattern) } ?: return false
        return regex.matches(url)
    }

    private fun compile(pattern: String): Regex? {
        val p = pattern.trim()
        if (p.isEmpty()) return null
        if (p == "<all_urls>") return Regex("^https?://.*$")

        // @match form: scheme://host/path (scheme ∈ http|https|file|*)
        val sep = p.indexOf("://")
        if (sep > 0) {
            val scheme = p.substring(0, sep)
            if (scheme == "http" || scheme == "https" || scheme == "file" || scheme == "*") {
                val rest = p.substring(sep + 3)
                val slash = rest.indexOf('/')
                val host = if (slash >= 0) rest.substring(0, slash) else rest
                val path = if (slash >= 0) rest.substring(slash) else "/"
                val sb = StringBuilder("^")
                sb.append(if (scheme == "*") "https?" else scheme).append("://")
                appendHostRegex(sb, host)
                appendGlob(sb, path)
                sb.append('$')
                return tryRegex(sb)
            }
        }

        if (p == "*") return Regex("^https?://.*$")

        // @include glob form over the full URL ("*" matches anything)
        val sb = StringBuilder("^")
        if (!p.contains("://") && !p.startsWith("*")) {
            // scheme-less host glob like "example.com/*"
            sb.append("https?://([^/]+\\.)?")
        }
        appendGlob(sb, p)
        sb.append('$')
        return tryRegex(sb)
    }

    private fun tryRegex(sb: StringBuilder): Regex? =
        try {
            Regex(sb.toString())
        } catch (e: Exception) {
            null
        }

    private fun appendHostRegex(sb: StringBuilder, host: String) {
        when {
            host == "*" -> sb.append("[^/]+")
            host.startsWith("*.") -> {
                sb.append("([^/]+\\.)?")
                escapeInto(sb, host.substring(2))
            }
            else -> escapeInto(sb, host)
        }
    }

    private fun appendGlob(sb: StringBuilder, glob: String) {
        for (c in glob) {
            if (c == '*') sb.append(".*") else escapeCharInto(sb, c)
        }
    }

    private fun escapeInto(sb: StringBuilder, s: String) {
        for (c in s) escapeCharInto(sb, c)
    }

    private fun escapeCharInto(sb: StringBuilder, c: Char) {
        if (!c.isLetterOrDigit() && c != '_' && c != '-') sb.append('\\')
        sb.append(c.lowercaseChar())
    }
}

package com.shieldbrowser.app.adblock

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.shieldbrowser.app.Prefs
import java.io.File
import java.io.InputStream
import java.util.Locale

/**
 * Multi-layer ad & tracker blocking engine.
 *
 * Sources (bundled in assets, or refreshed into filesDir by [FilterUpdater]):
 *   1. hosts.txt        – StevenBlack hosts (ad/malware/tracker domains)
 *   2. easylist.txt     – EasyList network rules + cosmetic filters
 *   3. easyprivacy.txt  – EasyPrivacy tracker rules
 *   4. annoyances.txt   – cookie/consent banner cosmetic rules
 *   5. extras.txt       – hand-written extra rules (YouTube etc.)
 *
 * Blocking layers:
 *   A. Plain domain set matching (O(1) host + parent-domain walk)
 *   B. ABP network filter rules (regex, n-gram token indexed for speed)
 *   C. Exception (@@) rules with option support ($third-party, types, domain=)
 *   D. Cosmetic filtering (element hiding) injected as CSS
 */
object AdBlockEngine {

    private const val TAG = "AdBlockEngine"
    private const val UPDATE_DIR = "filters_updated"
    private val FILES = listOf(
        "hosts.txt",
        "easylist.txt",
        "easyprivacy.txt",
        "annoyances.txt",
        "extras.txt"
    )
    private const val MAX_GENERIC_SELECTORS = 6000
    private const val NGRAM = 4

    // ---- resource-type bit flags (subset of ABP types) ----
    const val T_UNKNOWN = -1
    const val T_SCRIPT = 1
    const val T_IMAGE = 2
    const val T_CSS = 4
    const val T_XHR = 8
    const val T_SUBDOC = 16
    const val T_MEDIA = 32
    const val T_FONT = 64
    const val T_DOC = 128
    const val T_OTHER = 256

    data class Stats(
        val domains: Int = 0,
        val rules: Int = 0,
        val exceptions: Int = 0,
        val cosmetic: Int = 0,
        val source: String = "-",
        val loadMs: Long = 0
    )

    @Volatile
    var ready = false
        private set

    @Volatile
    var stats = Stats()
        private set

    /* counters */
    @Volatile var blockedThisPage = 0; private set
    @Volatile var blockedSession = 0; private set
    @Volatile var popupsBlockedThisPage = 0; private set

    fun onNewPage() {
        blockedThisPage = 0
        popupsBlockedThisPage = 0
    }

    fun onBlocked() {
        blockedThisPage++
        blockedSession++
        try {
            Prefs.blockedTotal = Prefs.blockedTotal + 1
        } catch (ignored: Exception) {
        }
    }

    fun onPopupBlocked() {
        popupsBlockedThisPage++
        onBlocked()
    }

    // ----------------------------------------------------------------------------------
    // Internal data structures
    // ----------------------------------------------------------------------------------

    private class Rule(
        val regex: Regex,
        val important: Boolean,
        val thirdParty: Boolean?,          // null = any party
        val typeIncl: Int,
        val typeExcl: Int,
        val domainIncl: List<String>?,     // from domain= option
        val domainExcl: List<String>?
    ) {
        fun matches(pageHost: String?, urlLower: String, isThirdParty: Boolean, type: Int): Boolean {
            if (thirdParty != null && thirdParty != isThirdParty) return false
            if (type != T_UNKNOWN) {
                if (typeIncl != 0 && typeIncl and type == 0) return false
                if (typeExcl != 0 && typeExcl and type != 0) return false
            }
            if (domainIncl != null) {
                if (pageHost == null) return false
                var ok = false
                for (d in domainIncl) if (hostSuffixMatch(pageHost, d)) { ok = true; break }
                if (!ok) return false
            }
            if (domainExcl != null && pageHost != null) {
                for (d in domainExcl) if (hostSuffixMatch(pageHost, d)) return false
            }
            return regex.containsMatchIn(urlLower)
        }
    }

    /** n-gram token index: rules are bucketed by the first [NGRAM] chars of their
     *  longest literal token; at match time we slide an [NGRAM]-window over the URL
     *  so only rules whose token prefix actually occurs are regex-tested. */
    private class RuleIndex {
        private val map = HashMap<String, ArrayList<Rule>>()
        private val noToken = ArrayList<Rule>()
        var size = 0
            private set

        fun add(rule: Rule, token: String?) {
            size++
            if (token == null || token.length < NGRAM) {
                noToken.add(rule)
            } else {
                map.getOrPut(token.substring(0, NGRAM)) { ArrayList() }.add(rule)
            }
        }

        /** True when [predicate] accepts any bucketed rule matching [urlLower]. */
        fun anyMatch(urlLower: String, predicate: (Rule) -> Boolean): Boolean {
            for (r in noToken) if (predicate(r)) return true
            if (map.isEmpty()) return false
            val n = urlLower.length
            var i = 0
            var lastHit: Rule? = null
            while (i + NGRAM <= n) {
                val bucket = map[urlLower.substring(i, i + NGRAM)]
                if (bucket != null) {
                    for (r in bucket) {
                        if (r === lastHit) continue
                        lastHit = r
                        if (predicate(r)) return true
                    }
                }
                i++
            }
            return false
        }
    }

    private fun hostSuffixMatch(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")

    // state (whole snapshot replaced atomically on (re)load)
    private class Snapshot {
        val domains = HashSet<String>(150_000)
        val exceptionDomains = HashSet<String>()
        val blockIndex = RuleIndex()
        val exceptionIndex = RuleIndex()
        val importantIndex = RuleIndex()
        val genericSelectors = LinkedHashSet<String>()
        val genericExceptions = HashSet<String>()
        val specificSelectors = HashMap<String, ArrayList<String>>()
        val specificExceptions = HashMap<String, ArrayList<String>>()
        var cosmeticCount = 0
    }

    @Volatile
    private var snap = Snapshot()

    @Volatile
    private var loading = false

    // ----------------------------------------------------------------------------------
    // Loading
    // ----------------------------------------------------------------------------------

    fun warmUp(context: Context) {
        if (ready || loading) return
        loading = true
        Thread {
            loadInternal(context.applicationContext)
        }.start()
    }

    /** Force a reload, e.g. after filter lists were updated from the network. */
    fun reload(context: Context) {
        ready = false
        Thread {
            loading = true
            loadInternal(context.applicationContext)
        }.start()
    }

    private fun openFilter(context: Context, name: String): InputStream? {
        val updated = File(File(context.filesDir, UPDATE_DIR), name)
        try {
            if (updated.exists() && updated.length() > 0) {
                return updated.inputStream()
            }
        } catch (ignored: Exception) {
        }
        return try {
            context.assets.open("filters/$name")
        } catch (e: Exception) {
            null
        }
    }

    fun updatedOn(context: Context): String? {
        return try {
            val f = File(File(context.filesDir, UPDATE_DIR), "VERSION.txt")
            if (f.exists() && f.length() > 0) "updated ${f.readText().trim()}" else null
        } catch (e: Exception) {
            null
        }
    }

    private fun loadInternal(context: Context) {
        val t0 = SystemClock.elapsedRealtime()
        val s = Snapshot()
        var source = "bundled"
        try {
            for (name in FILES) {
                val stream = openFilter(context, name) ?: continue
                if (source == "bundled" &&
                    File(File(context.filesDir, UPDATE_DIR), name).let { it.exists() && it.length() > 0 }
                ) {
                    source = "updated"
                }
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.forEachLine { raw -> parseLine(raw, s) }
                }
            }
            // apply cosmetic exceptions
            for (ex in s.genericExceptions) s.genericSelectors.remove(ex)
            for ((d, exs) in s.specificExceptions) {
                s.specificSelectors[d]?.let { list ->
                    list.removeAll(exs.toSet())
                    if (list.isEmpty()) s.specificSelectors.remove(d)
                }
            }
            s.cosmeticCount = s.genericSelectors.size +
                    s.specificSelectors.values.sumOf { l -> l.size }
            snap = s
            stats = Stats(
                domains = s.domains.size,
                rules = s.blockIndex.size,
                exceptions = s.exceptionIndex.size + s.exceptionDomains.size,
                cosmetic = s.cosmeticCount,
                source = source,
                loadMs = SystemClock.elapsedRealtime() - t0
            )
            ready = true
            Log.i(
                TAG,
                "Loaded: ${stats.domains} domains, ${stats.rules} rules, " +
                        "${stats.exceptions} exceptions, ${stats.cosmetic} cosmetics in ${stats.loadMs}ms"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load filters", e)
        } finally {
            loading = false
        }
    }

    // ----------------------------------------------------------------------------------
    // Parsing
    // ----------------------------------------------------------------------------------

    private val domainRegex = Regex("^[a-z0-9]([a-z0-9.\\-_]*[a-z0-9])?\\.[a-z]{2,24}$")

    private fun isValidDomain(d: String): Boolean =
        d.length in 4..253 && !d.contains("..") && domainRegex.matches(d)

    private val localNames = setOf(
        "localhost", "localhost.localdomain", "local", "broadcasthost",
        "ip6-localhost", "ip6-loopback", "ip6-localnet", "ip6-mcastprefix",
        "ip6-allnodes", "ip6-allrouters", "ip6-allhosts"
    )

    private fun parseLine(raw: String, s: Snapshot) {
        val line = raw.trim()
        if (line.isEmpty()) return
        val c0 = line[0]
        if (c0 == '!' || c0 == '[' || c0 == '#') return

        // hosts file format: "0.0.0.0 doubleclick.net" (IP + whitespace + domain)
        if (line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ")) {
            val host = line.substringAfter(' ').trim()
                .substringBefore(' ').substringBefore('\t')
                .substringBefore('#').trim().lowercase(Locale.US)
            if (isValidDomain(host) && host !in localNames) s.domains.add(host)
            return
        }

        // cosmetic filters
        val idxHide = line.indexOf("##")
        val idxAllow = line.indexOf("#@#")
        if (idxHide >= 0 && (idxAllow < 0 || idxHide < idxAllow)) {
            parseCosmetic(line, s, isException = false, idx = idxHide)
            return
        }
        if (idxAllow >= 0 && (idxHide < 0 || idxAllow < idxHide)) {
            parseCosmetic(line, s, isException = true, idx = idxAllow)
            return
        }

        // exception marker
        var isException = false
        var work = line
        if (work.startsWith("@@")) {
            isException = true
            work = work.substring(2)
        }

        // split pattern / options (options follow the first '$')
        var options: String? = null
        val dollar = work.indexOf('$')
        if (dollar > 0) {
            options = work.substring(dollar + 1)
            work = work.substring(0, dollar)
        }
        if (work.isEmpty()) return

        val parsed = parseOptions(options) ?: return  // unsupported option -> drop rule

        // pure domain rules (||domain^ with no options) -> O(1) domain set
        if (work.startsWith("||") && work.endsWith("^") && work.length > 4 &&
            work.indexOf('^') == work.length - 1 &&
            !work.contains('*') && !work.contains('/') && parsed.isPlain
        ) {
            val d = work.substring(2, work.length - 1).lowercase(Locale.US)
            if (isValidDomain(d)) {
                if (isException) s.exceptionDomains.add(d) else s.domains.add(d)
                return
            }
        }

        val regex = abpToRegex(work) ?: return
        val rule = Rule(
            regex = regex,
            important = parsed.important,
            thirdParty = parsed.thirdParty,
            typeIncl = parsed.typeIncl,
            typeExcl = parsed.typeExcl,
            domainIncl = parsed.domainIncl,
            domainExcl = parsed.domainExcl
        )
        val token = extractToken(work)
        when {
            isException -> s.exceptionIndex.add(rule, token)
            parsed.important -> s.importantIndex.add(rule, token)
            else -> s.blockIndex.add(rule, token)
        }
    }

    private class ParsedOptions(
        val important: Boolean = false,
        val thirdParty: Boolean? = null,
        val typeIncl: Int = 0,
        val typeExcl: Int = 0,
        val domainIncl: List<String>? = null,
        val domainExcl: List<String>? = null
    ) {
        val isPlain: Boolean
            get() = !important && thirdParty == null && typeIncl == 0 && typeExcl == 0
    }

    private val typeMap = mapOf(
        "script" to T_SCRIPT,
        "image" to T_IMAGE,
        "stylesheet" to T_CSS,
        "css" to T_CSS,
        "xmlhttprequest" to T_XHR,
        "xhr" to T_XHR,
        "subdocument" to T_SUBDOC,
        "frame" to T_SUBDOC,
        "media" to T_MEDIA,
        "font" to T_FONT,
        "document" to T_DOC,
        "other" to T_OTHER,
        "object" to T_OTHER,
        "ping" to T_XHR,
        "beacon" to T_XHR,
        "websocket" to T_XHR,
        "object-subrequest" to T_OTHER
    )

    /** Options we deliberately do not implement – rules carrying them are dropped. */
    private val unsupportedOptions = setOf(
        "popup", "badfilter", "csp", "redirect", "rewrite", "removeparam",
        "replace", "generichide", "genericblock", "elemhide", "jsinject",
        "sitekey", "donottrack", "webrtc", "prefetch", "header", "method",
        "to", "from", "denyallow", "cookie", "network", "match-case",
        "redirect-rule", "all", "content"
    )

    private fun parseOptions(raw: String?): ParsedOptions? {
        if (raw == null) return ParsedOptions()
        var important = false
        var thirdParty: Boolean? = null
        var typeIncl = 0
        var typeExcl = 0
        var domainIncl: MutableList<String>? = null
        var domainExcl: MutableList<String>? = null
        for (piece0 in raw.split(',')) {
            val piece = piece0.trim().lowercase(Locale.US)
            if (piece.isEmpty()) continue
            when {
                piece == "important" -> important = true
                piece == "third-party" -> thirdParty = true
                piece == "~third-party" -> thirdParty = false
                piece == "first-party" -> thirdParty = false
                piece.startsWith("domain=") -> {
                    for (d0 in piece.substring(7).split('|')) {
                        val d = d0.trim()
                        if (d.isEmpty()) continue
                        if (d.startsWith("~")) {
                            if (domainExcl == null) domainExcl = ArrayList()
                            domainExcl.add(d.substring(1))
                        } else {
                            if (domainIncl == null) domainIncl = ArrayList()
                            domainIncl.add(d)
                        }
                    }
                }
                piece.startsWith("~") -> {
                    val t = typeMap[piece.substring(1)] ?: return null
                    typeExcl = typeExcl or t
                }
                else -> {
                    val t = typeMap[piece]
                    when {
                        t != null -> typeIncl = typeIncl or t
                        else -> return null   // unknown/unsupported option -> drop rule
                    }
                }
            }
        }
        return ParsedOptions(
            important = important,
            thirdParty = thirdParty,
            typeIncl = typeIncl,
            typeExcl = typeExcl,
            domainIncl = domainIncl,
            domainExcl = domainExcl
        )
    }

    private val badSelectorParts = listOf(
        ":-abp-", "-abp-", ":xpath", ":style(", ":matches-css", ":remove",
        ":if(", ":if-not(", ":has-text(", ":contains(", ":upward", "+js(",
        "script:contains", ":properties(", ":watch-attr", ":min-text-length",
        ":has(", ":nth-ancestor"
    )

    private fun isValidSelector(sel: String): Boolean {
        if (sel.isEmpty() || sel.length > 400) return false
        if (sel.contains("##") || sel.contains("#@#")) return false
        if (sel.contains('{') || sel.contains('}') || sel.contains(';')) return false
        for (bad in badSelectorParts) if (sel.contains(bad)) return false
        return true
    }

    private fun parseCosmetic(line: String, s: Snapshot, isException: Boolean, idx: Int) {
        val sepLen = if (isException) 3 else 2
        val domainsPart = line.substring(0, idx)
        val sel = line.substring(idx + sepLen).trim()
        if (!isValidSelector(sel)) return
        if (domainsPart.isEmpty()) {
            if (isException) {
                s.genericExceptions.add(sel)
            } else if (s.genericSelectors.size < MAX_GENERIC_SELECTORS) {
                s.genericSelectors.add(sel)
            }
            return
        }
        if (domainsPart.contains('~') || domainsPart.contains('*')) return
        for (d0 in domainsPart.split(',')) {
            val d = d0.trim().lowercase(Locale.US)
            if (d.isEmpty() || !isValidDomain(d)) continue
            if (isException) {
                s.specificExceptions.getOrPut(d) { ArrayList() }.add(sel)
            } else {
                s.specificSelectors.getOrPut(d) { ArrayList() }.add(sel)
            }
        }
    }

    /** Translates an ABP URL pattern into a Java/Kotlin regex. */
    private fun abpToRegex(pattern: String): Regex? {
        val sb = StringBuilder(pattern.length + 16)
        var s = pattern
        when {
            s.startsWith("||") -> {
                sb.append("^[a-z][a-z0-9+.-]*://([^/?#]*\\.)?")
                s = s.substring(2)
            }
            s.startsWith("|") -> {
                sb.append('^')
                s = s.substring(1)
            }
        }
        val last = s.length - 1
        var i = 0
        while (i <= last) {
            val c = s[i]
            when (c) {
                '*' -> sb.append(".*")
                '^' -> sb.append("(?:[\\x00-\\x2C\\x2F\\x3A-\\x40\\x5B-\\x5E\\x60\\x7B-\\x7F]|${'$'})")
                '|' -> if (i == last) sb.append('$') else return null
                else -> {
                    if ("\\.[]{}()+-?,\"%&=\$".indexOf(c) >= 0) sb.append('\\')
                    sb.append(c.lowercaseChar())
                }
            }
            i++
        }
        return try {
            Regex(sb.toString())
        } catch (e: Exception) {
            null
        }
    }

    /** Longest alphanumeric token of an ABP pattern – used as bucket key. */
    private val tokenSplit = Regex("[^A-Za-z0-9]+")

    private fun extractToken(pattern: String): String? {
        var best: String? = null
        for (p in pattern.split(tokenSplit)) {
            if (p.length >= 5 && (best == null || p.length > best.length)) best = p
        }
        return best?.lowercase(Locale.US)
    }

    // ----------------------------------------------------------------------------------
    // Matching
    // ----------------------------------------------------------------------------------

    private fun hostOf(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        return try {
            Uri.parse(url).host?.lowercase(Locale.US)
        } catch (e: Exception) {
            null
        }
    }

    private fun walkHosts(host: String, hit: (String) -> Boolean): Boolean {
        var h = host
        while (true) {
            if (hit(h)) return true
            val dot = h.indexOf('.')
            if (dot < 0 || dot == h.length - 1) return false
            h = h.substring(dot + 1)
        }
    }

    private fun isThirdParty(pageHost: String?, reqHost: String?): Boolean {
        if (pageHost == null || reqHost == null) return false
        return !hostSuffixMatch(reqHost, pageHost) && !hostSuffixMatch(pageHost, reqHost)
    }

    /**
     * The main blocking decision.
     * @param pageUrl URL of the page that triggered the request (may be null)
     * @param requestUrl the URL being requested
     * @param isMainFrame true when the request is for the top-level document
     */
    fun isBlocked(pageUrl: String?, requestUrl: String, isMainFrame: Boolean): Boolean {
        if (!ready) return false
        if (!requestUrl.startsWith("http")) return false
        val s = snap
        val reqHost = hostOf(requestUrl) ?: return false

        if (isMainFrame) {
            // Top-level documents: only flat blocked domains apply, so we
            // never break deliberate navigation.
            return walkHosts(reqHost) { s.domains.contains(it) } &&
                    !walkHosts(reqHost) { s.exceptionDomains.contains(it) }
        }

        val pageHost = hostOf(pageUrl)
        val urlLower = requestUrl.lowercase(Locale.US)
        val third = isThirdParty(pageHost, reqHost)
        val type = guessType(urlLower)

        fun exceptionsApply(): Boolean =
            s.exceptionIndex.anyMatch(urlLower) { it.matches(pageHost, urlLower, third, type) }

        fun importantApplies(): Boolean =
            s.importantIndex.anyMatch(urlLower) { it.matches(pageHost, urlLower, third, type) }

        // 1) fast domain blocking
        if (walkHosts(reqHost) { s.domains.contains(it) }) {
            if (walkHosts(reqHost) { s.exceptionDomains.contains(it) }) return false
            if (exceptionsApply()) return importantApplies()
            return true
        }

        // 2) exception rules beat pattern rules
        if (exceptionsApply()) return importantApplies()

        // 3) pattern rules
        if (s.blockIndex.anyMatch(urlLower) { it.matches(pageHost, urlLower, third, type) }) return true
        return importantApplies()
    }

    private fun guessType(urlLower: String): Int {
        val q = urlLower.indexOf('?')
        val path = if (q >= 0) urlLower.substring(0, q) else urlLower
        val dot = path.lastIndexOf('.')
        if (dot < 0 || path.length - dot > 6) return T_UNKNOWN
        return when (path.substring(dot + 1)) {
            "js" -> T_SCRIPT
            "css" -> T_CSS
            "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "avif", "apng", "bmp" -> T_IMAGE
            "woff", "woff2", "ttf", "otf", "eot" -> T_FONT
            "mp4", "webm", "m3u8", "mp3", "ogg", "wav", "aac", "flac", "mpd", "ts" -> T_MEDIA
            "json", "xml" -> T_XHR
            "html", "htm", "php", "aspx" -> T_SUBDOC
            else -> T_UNKNOWN
        }
    }

    // ----------------------------------------------------------------------------------
    // Cosmetic filtering
    // ----------------------------------------------------------------------------------

    /** All selectors that apply to [host] (generic + site-specific). */
    fun cosmeticSelectorsFor(host: String): List<String> {
        val s = snap
        val out = ArrayList<String>(s.genericSelectors.size + 32)
        out.addAll(s.genericSelectors)
        var h = host
        while (true) {
            s.specificSelectors[h]?.let { out.addAll(it) }
            val dot = h.indexOf('.')
            if (dot < 0 || dot == h.length - 1) break
            h = h.substring(dot + 1)
        }
        return out
    }
}

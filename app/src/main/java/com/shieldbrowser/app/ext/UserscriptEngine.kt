package com.shieldbrowser.app.ext

import android.content.Context
import android.webkit.WebView

/**
 * Matches installed userscripts against URLs and builds the injection
 * wrappers (with a small Greasemonkey-compatible GM API shim).
 */
object UserscriptEngine {

    @Volatile
    private var cached: List<Userscript>? = null

    @Volatile
    private var loading = false

    fun warmUp(context: Context) {
        ExtensionStore.init(context)
        kickAsyncLoad()
    }

    /** Call after install/remove/toggle so the next page load sees changes. */
    fun invalidate() {
        cached = null
    }

    private fun kickAsyncLoad() {
        if (loading) return
        loading = true
        Thread {
            cached = try {
                ExtensionStore.all()
            } catch (t: Throwable) {
                com.shieldbrowser.app.CrashGuard.record("userscripts-load", t)
                emptyList()
            }
            loading = false
        }.start()
    }

    private fun scripts(): List<Userscript> {
        cached?.let { return it }
        // never do disk IO on the page-loading thread: load in background
        // and simply skip injections until the cache is warm
        kickAsyncLoad()
        return emptyList()
    }

    /** Injects all matching scripts into [webView] for the given lifecycle stage. */
    fun inject(webView: WebView, url: String?, stage: Userscript.RunAt) {
        if (url.isNullOrEmpty() || !url.startsWith("http")) return
        val active = scripts().filter { it.enabled && it.runAt == stage && it.matches(url) }
        for (script in active) {
            try {
                webView.evaluateJavascript(buildInjection(script), null)
            } catch (ignored: Throwable) {
            }
        }
    }

    /** JS wrapper: defines GM_addStyle / GM_setValue / GM_getValue / GM_info … */
    fun buildInjection(script: Userscript): String {
        val sb = StringBuilder(script.source.length + 1024)
        sb.append("(function(){try{")
        sb.append("var GM_VALUES=").append(ExtensionStore.valuesJson(script.id)).append(";")
        sb.append("function GM_addStyle(css){")
        sb.append("var s=document.createElement('style');s.textContent=css;")
        sb.append("(document.head||document.documentElement).appendChild(s);};")
        sb.append("function GM_setValue(k,v){GM_VALUES[k]=v;")
        sb.append("try{ShieldExt.setValue(")
        appendJsString(sb, script.id)
        sb.append(",k,JSON.stringify(v===undefined?null:v));}catch(e){}};")
        sb.append("function GM_getValue(k,d){return (k in GM_VALUES)?GM_VALUES[k]:d;};")
        sb.append("function GM_deleteValue(k){delete GM_VALUES[k];")
        sb.append("try{ShieldExt.deleteValue(")
        appendJsString(sb, script.id)
        sb.append(",k);}catch(e){}};")
        sb.append("function GM_listValues(){return Object.keys(GM_VALUES);};")
        sb.append("function GM_log(m){console.log('[ShieldExt:'+")
        appendJsString(sb, script.name.take(24))
        sb.append("+'] '+m);};")
        sb.append("var GM={addStyle:GM_addStyle,setValue:function(k,v){return Promise.resolve(GM_setValue(k,v));},")
        sb.append("getValue:function(k,d){return Promise.resolve(GM_getValue(k,d));},")
        sb.append("deleteValue:function(k){return Promise.resolve(GM_deleteValue(k));},")
        sb.append("listValues:function(){return Promise.resolve(GM_listValues());},")
        sb.append("log:GM_log,info:null};")
        sb.append("var GM_info={script:{name:")
        appendJsString(sb, script.name)
        sb.append(",version:")
        appendJsString(sb, script.version)
        sb.append(",namespace:")
        appendJsString(sb, script.namespace)
        sb.append("},scriptHandler:'ShieldBrowser',version:'1.1'};")
        sb.append("GM.info=GM_info;")
        sb.append("var unsafeWindow=window;")

        // @require libraries are prepended (jQuery etc.)
        for (req in script.requires) {
            val lib = ExtensionStore.requiredSource(req) ?: continue
            sb.append("\ntry{\n").append(lib).append("\n}catch(e){console.log('[ShieldExt] require failed: ")
            appendJsString(sb, req.take(40))
            sb.append(" '+e);}\n")
        }

        sb.append("\n/* userscript body */\n")
        sb.append(script.source)
        sb.append("\n}catch(e){console.log('[ShieldExt] script error: '+e);}})();")
        return sb.toString()
    }

    private fun appendJsString(sb: StringBuilder, value: String) {
        sb.append('"')
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }
}

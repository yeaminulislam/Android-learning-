package com.shieldbrowser.app.ext

import android.webkit.JavascriptInterface

/** JavascriptInterface backing GM_setValue/GM_deleteValue in injected scripts. */
class ExtBridge {

    @JavascriptInterface
    fun setValue(scriptId: String, key: String, jsonValue: String) {
        try {
            ExtensionStore.putValue(scriptId, key, jsonValue)
        } catch (ignored: Throwable) {
        }
    }

    @JavascriptInterface
    fun deleteValue(scriptId: String, key: String) {
        try {
            ExtensionStore.deleteValue(scriptId, key)
        } catch (ignored: Throwable) {
        }
    }
}

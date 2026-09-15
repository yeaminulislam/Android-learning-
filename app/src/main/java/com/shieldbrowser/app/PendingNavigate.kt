package com.shieldbrowser.app

/** Tiny hand-off object: list activities (bookmarks/history) set a URL here,
 *  and MainActivity picks it up in onResume and opens it in a new tab. */
object PendingNavigate {
    @Volatile
    var url: String? = null
}

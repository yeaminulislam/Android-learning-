package com.shieldbrowser.app.browser

import com.shieldbrowser.app.MainActivity

/** Holds all open tabs and the current selection. */
object TabManager {

    interface Listener {
        fun onCurrentTabChanged(tab: BrowserTab)
        fun onTabCountChanged(count: Int)
    }

    val tabs = ArrayList<BrowserTab>()

    var listener: Listener? = null

    /** Set when tabs were mutated from another activity so MainActivity
     *  can re-sync its visible WebView. */
    @Volatile
    var dirty = false

    private var currentIndex = -1

    fun current(): BrowserTab? = tabs.getOrNull(currentIndex)

    fun currentPosition(): Int = currentIndex

    fun indexOf(tab: BrowserTab): Int = tabs.indexOf(tab)

    @Synchronized
    fun createTab(activity: MainActivity, url: String? = null, switch: Boolean = true): BrowserTab {
        val tab = BrowserTab(activity, url)
        tabs.add(tab)
        if (switch || currentIndex < 0) currentIndex = tabs.size - 1
        notifyChanged()
        return tab
    }

    @Synchronized
    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val removed = tabs.removeAt(index)
        removed.destroy()
        when {
            tabs.isEmpty() -> currentIndex = -1
            currentIndex >= tabs.size -> currentIndex = tabs.size - 1
            index < currentIndex -> currentIndex--
        }
        dirty = true
        notifyChanged()
    }

    @Synchronized
    fun select(index: Int) {
        if (index in tabs.indices) {
            currentIndex = index
            dirty = true
            notifyChanged()
        }
    }

    /** Close every tab except [index]. */
    @Synchronized
    fun closeOthers(index: Int) {
        if (index !in tabs.indices) return
        for (i in tabs.indices.reversed()) {
            if (i != index) {
                tabs[i].destroy()
                tabs.removeAt(i)
            }
        }
        currentIndex = 0
        dirty = true
        notifyChanged()
    }

    @Synchronized
    fun destroyAll() {
        for (t in tabs) t.destroy()
        tabs.clear()
        currentIndex = -1
    }

    private fun notifyChanged() {
        val l = listener ?: return
        l.onTabCountChanged(tabs.size)
        current()?.let { l.onCurrentTabChanged(it) }
    }
}

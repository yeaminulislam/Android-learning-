package com.shieldbrowser.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.shieldbrowser.app.adblock.AdBlockEngine
import com.shieldbrowser.app.browser.BrowserChromeClient
import com.shieldbrowser.app.browser.BrowserClient
import com.shieldbrowser.app.browser.BrowserTab
import com.shieldbrowser.app.browser.MediaController
import com.shieldbrowser.app.browser.TabManager
import com.shieldbrowser.app.browser.WebViewFactory
import com.shieldbrowser.app.ext.ExtensionStore
import com.shieldbrowser.app.ext.Userscript
import com.shieldbrowser.app.ext.UserscriptEngine
import com.shieldbrowser.app.store.BookmarkStore
import com.shieldbrowser.app.store.HistoryStore
import com.shieldbrowser.app.yt.YouTubeExtractor
import com.shieldbrowser.app.yt.YoutubeJsEngine
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity(),
    BrowserClient.Host,
    BrowserChromeClient.Host,
    TabManager.Listener {

    companion object {
        /** Needed by TabsActivity to create real tabs (WebViews need our context). */
        var instance: WeakReference<MainActivity>? = null
    }

    private lateinit var toolbar: Toolbar
    private lateinit var urlBar: EditText
    private lateinit var tabCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var webContainer: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private var ytEngine: YoutubeJsEngine? = null

    private var shieldToastShown = false
    private val handler = Handler(Looper.getMainLooper())

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = filePathCallback ?: return@registerForActivityResult
            filePathCallback = null
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            callback.onReceiveValue(uris)
        }

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        instance = WeakReference(this)

        toolbar = findViewById(R.id.toolbar)
        urlBar = findViewById(R.id.urlBar)
        tabCount = findViewById(R.id.tabCount)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webContainer = findViewById(R.id.webContainer)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        MediaController.hostActivity = WeakReference(this)
        TabManager.listener = this

        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_SEARCH
            ) {
                navigateTo(urlBar.text.toString())
                true
            } else {
                false
            }
        }

        tabCount.setOnClickListener {
            startActivity(Intent(this, TabsActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            TabManager.current()?.webView?.reload()
        }

        if (TabManager.tabs.isEmpty()) {
            val startUrl = when {
                intent?.action == Intent.ACTION_VIEW -> intent.dataString
                else -> null
            }
            TabManager.createTab(this, startUrl ?: BrowserApp.HOME_URL)
        } else {
            TabManager.current()?.let { attachTab(it) }
            tabCount.text = TabManager.tabs.size.toString()
        }

        setupBackPress()
        maybeAskNotificationPermission()
        watchShieldReady()
        // If the app crashed last time, offer the log to copy/share.
        CrashGuard.maybeShowPending(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            TabManager.createTab(this, intent.dataString)
        }
    }

    override fun onResume() {
        super.onResume()
        MediaController.hostActivity = WeakReference(this)
        if (TabManager.dirty) {
            TabManager.dirty = false
            TabManager.current()?.let { attachTab(it) }
            tabCount.text = TabManager.tabs.size.toString()
        }
        PendingNavigate.url?.let {
            PendingNavigate.url = null
            TabManager.createTab(this, it)
        }
        TabManager.current()?.webView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        // NOTE: for background playback we deliberately do NOT call
        // WebView.onPause() — that would freeze the media pipeline.
        if (!Prefs.backgroundPlay) {
            TabManager.current()?.webView?.onPause()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopService(Intent(this, PlaybackService::class.java))
            ytEngine?.destroy()
            ytEngine = null
            TabManager.destroyAll()
        }
        instance = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    // =========================================================================
    // Back press
    // =========================================================================

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    fullscreenView != null -> uiExitFullscreen()
                    else -> {
                        val current = TabManager.current()?.webView
                        if (current?.canGoBack() == true) {
                            current.goBack()
                        } else {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        })
    }

    // =========================================================================
    // Navigation helpers
    // =========================================================================

    private fun navigateTo(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) return
        val url = if (looksLikeUrl(input)) withScheme(input) else searchUrl(input)
        TabManager.current()?.webView?.loadUrl(url)
        hideKeyboard()
        urlBar.clearFocus()
    }

    private fun looksLikeUrl(input: String): Boolean {
        if (input.startsWith("http://") || input.startsWith("https://") ||
            input.startsWith("file://")
        ) return true
        if (input.contains(' ')) return false
        return input.contains('.') || input.startsWith("localhost")
    }

    private fun withScheme(input: String): String {
        return if (input.startsWith("http://") || input.startsWith("https://") ||
            input.startsWith("file://")
        ) input else "https://$input"
    }

    private fun searchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return when (Prefs.searchEngine) {
            "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
            "bing" -> "https://www.bing.com/search?q=$encoded"
            "brave" -> "https://search.brave.com/search?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
    }

    // =========================================================================
    // TabManager.Listener
    // =========================================================================

    override fun onCurrentTabChanged(tab: BrowserTab) {
        attachTab(tab)
    }

    override fun onTabCountChanged(count: Int) {
        tabCount.text = count.toString()
    }

    private fun attachTab(tab: BrowserTab) {
        webContainer.removeAllViews()
        (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
        webContainer.addView(tab.webView)
        if (!urlBar.hasFocus()) {
            urlBar.setText(tab.url ?: "")
        }
    }

    private fun isCurrent(tab: BrowserTab): Boolean = TabManager.current() === tab

    // =========================================================================
    // BrowserClient.Host
    // =========================================================================

    override fun activityOrNull(): MainActivity = this

    override fun uiPageStarted(tab: BrowserTab, url: String?) {
        if (!isCurrent(tab)) return
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE
        if (!urlBar.hasFocus() && !url.isNullOrEmpty()) {
            urlBar.setText(url)
        }
    }

    override fun uiPageFinished(tab: BrowserTab, url: String?) {
        swipeRefresh.isRefreshing = false
        if (isCurrent(tab)) {
            progressBar.visibility = View.GONE
            if (!urlBar.hasFocus() && !url.isNullOrEmpty()) {
                urlBar.setText(url)
            }
        }
        if (!url.isNullOrEmpty() && url.startsWith("http")) {
            HistoryStore.add(tab.title, url)
        }
    }

    override fun uiUrlChanged(tab: BrowserTab, url: String?) {
        if (isCurrent(tab) && !urlBar.hasFocus() && !url.isNullOrEmpty()) {
            urlBar.setText(url)
        }
    }

    override fun uiOpenInNewTab(url: String) {
        TabManager.createTab(this, url)
    }

    override fun uiRenderProcessGone(tab: BrowserTab) {
        tab.recreate()
        if (isCurrent(tab)) attachTab(tab)
    }

    override fun uiSearch(query: String) {
        if (query.isNotBlank()) navigateTo(query)
    }

    override fun uiUserscriptFound(url: String) {
        fetchAndOfferUserscript(url)
    }

    // =========================================================================
    // Userscript (extension) installation
    // =========================================================================

    private fun fetchAndOfferUserscript(url: String) {
        toast(getString(R.string.fetching_video))
        Thread {
            val source = try {
                downloadText(url, 2 * 1024 * 1024)
            } catch (t: Throwable) {
                CrashGuard.record("fetch-userscript", t)
                null
            }
            handler.post {
                if (source == null) {
                    toast(getString(R.string.script_install_failed))
                } else {
                    try {
                        offerUserscriptInstall(source)
                    } catch (t: Throwable) {
                        CrashGuard.record("offer-userscript", t)
                        toast(getString(R.string.script_install_failed))
                    }
                }
            }
        }.start()
    }

    private fun offerUserscriptInstall(source: String) {
        val parsed = Userscript.parse("preview", source)
        if (parsed == null) {
            toast(getString(R.string.script_install_failed))
            return
        }
        val info = buildString {
            append("📜 ").append(parsed.name).append("  v").append(parsed.version).append("\n")
            if (parsed.description.isNotBlank()) append(parsed.description).append("\n")
            append(getString(R.string.script_matches_label)).append(' ')
            append(parsed.includes.take(4).joinToString(", "))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.script_install_title)
            .setMessage(info)
            .setPositiveButton(R.string.script_install_btn) { _, _ ->
                val installed = ExtensionStore.install(source)
                if (installed != null) {
                    UserscriptEngine.invalidate()
                    toast(getString(R.string.script_installed) + "  " + installed.name)
                } else {
                    toast(getString(R.string.script_install_failed))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // =========================================================================
    // YouTube video download
    // =========================================================================

    private fun jsEngine(): YoutubeJsEngine {
        if (ytEngine == null) ytEngine = YoutubeJsEngine(android.webkit.WebView(this))
        return ytEngine!!
    }

    private fun startVideoDownload() {
        val url = TabManager.current()?.webView?.url
        if (!YouTubeExtractor.looksLikeWatchUrl(url)) {
            toast(getString(R.string.not_youtube_page))
            return
        }
        toast(getString(R.string.fetching_video))
        YouTubeExtractor.extract(url!!, jsEngine(), object : YouTubeExtractor.Callback {
            override fun onResult(result: YouTubeExtractor.Result) {
                if (!result.ok) {
                    toast(getString(R.string.video_fetch_failed))
                    return
                }
                showFormatPicker(result)
            }
        })
    }

    private fun showFormatPicker(result: YouTubeExtractor.Result) {
        val labels = result.streams.map { stream ->
            buildString {
                append(stream.qualityLabel).append(" · ").append(stream.container)
                append(" · ").append(
                    when {
                        stream.hasVideo && stream.hasAudio -> getString(R.string.dl_with_audio)
                        stream.hasVideo -> getString(R.string.dl_no_audio)
                        else -> getString(R.string.dl_audio_only)
                    }
                )
                append(" · ").append(
                    if (stream.contentLength > 0) formatBytes(stream.contentLength)
                    else getString(R.string.dl_unknown_size)
                )
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_quality) + "\n" + result.title)
            .setItems(labels) { _, which ->
                enqueueYoutubeDownload(result, result.streams[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun enqueueYoutubeDownload(
        result: YouTubeExtractor.Result,
        stream: YouTubeExtractor.Stream
    ) {
        try {
            val title = result.title.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
                .ifBlank { result.videoId }
            val ext = when (stream.container) {
                "MP4" -> "mp4"; "WEBM" -> "webm"; "M4A" -> "m4a"
                else -> "bin"
            }
            val fileName = "$title [${stream.qualityLabel}].$ext"
            val request = android.app.DownloadManager.Request(Uri.parse(stream.url)).apply {
                setTitle(fileName)
                setDescription(getString(R.string.app_name))
                addRequestHeader(
                    "User-Agent",
                    TabManager.current()?.webView?.settings?.userAgentString ?: "Mozilla/5.0"
                )
                addRequestHeader("Referer", "https://www.youtube.com/watch?v=" + result.videoId)
                setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS, fileName
                )
            }
            val dm = getSystemService(android.app.DownloadManager::class.java)
            dm?.enqueue(request)
            toast(getString(R.string.download_started) + "  " + stream.qualityLabel)
        } catch (e: Exception) {
            toast(getString(R.string.cant_open_link))
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "?"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = bytes.toDouble()
        var u = 0
        while (v >= 1024 && u < units.size - 1) {
            v /= 1024; u++
        }
        return String.format("%.1f %s", v, units[u])
    }

    private fun downloadText(url: String, cap: Int): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ShieldBrowser/1.1")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(32 * 1024)
                var total = 0
                while (true) {
                    val r = input.read(buf)
                    if (r <= 0) break
                    total += r
                    if (total > cap) break
                    out.write(buf, 0, r)
                }
                out.toString(Charsets.UTF_8.name())
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun toast(message: String) {
        if (isFinishing) return
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // =========================================================================
    // BrowserChromeClient.Host
    // =========================================================================

    override fun uiProgress(progress: Int) {
        progressBar.progress = progress
        progressBar.visibility = if (progress < 100) View.VISIBLE else View.GONE
    }

    override fun uiTitle(tab: BrowserTab, title: String?) {
        // title already stored on the tab; nothing else to update live
    }

    override fun uiShowFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        return try {
            fileChooserLauncher.launch(params.createIntent())
            true
        } catch (e: Exception) {
            filePathCallback = null
            false
        }
    }

    override fun uiEnterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (fullscreenView != null) {
            callback.onCustomViewHidden()
            return
        }
        fullscreenView = view
        fullscreenCallback = callback
        fullscreenContainer.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        fullscreenContainer.visibility = View.VISIBLE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun uiExitFullscreen() {
        val view = fullscreenView ?: return
        fullscreenContainer.removeView(view)
        fullscreenContainer.visibility = View.GONE
        fullscreenView = null
        try {
            fullscreenCallback?.onCustomViewHidden()
        } catch (ignored: Exception) {
        }
        fullscreenCallback = null
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun uiPopupBlocked() {
        Toast.makeText(this, R.string.blocked_popup, Toast.LENGTH_SHORT).show()
    }

    // =========================================================================
    // Menu
    // =========================================================================

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_background_play)?.isChecked = Prefs.backgroundPlay
        menu.findItem(R.id.menu_desktop)?.isChecked = Prefs.desktopMode
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val current = TabManager.current()
        when (item.itemId) {
            R.id.menu_forward -> current?.webView?.goForward()
            R.id.menu_reload -> current?.webView?.reload()
            R.id.menu_home -> current?.webView?.loadUrl(BrowserApp.HOME_URL)
            R.id.menu_add_bookmark -> addBookmark()
            R.id.menu_download -> startVideoDownload()
            R.id.menu_extensions -> startActivity(Intent(this, ExtensionsActivity::class.java))
            R.id.menu_bookmarks -> startActivity(Intent(this, BookmarksActivity::class.java))
            R.id.menu_history -> startActivity(Intent(this, HistoryActivity::class.java))
            R.id.menu_shield -> showShieldInfo()
            R.id.menu_background_play -> toggleBackgroundPlay(item)
            R.id.menu_desktop -> toggleDesktopMode(item)
            R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.menu_exit -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun addBookmark() {
        val tab = TabManager.current() ?: return
        val url = tab.url ?: return
        if (!url.startsWith("http") && url != BrowserApp.HOME_URL) return
        val title = tab.title.ifBlank { url }
        val added = BookmarkStore.toggle(title, url)
        Toast.makeText(
            this,
            if (added) R.string.bookmark_added else R.string.bookmark_removed,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleBackgroundPlay(item: MenuItem) {
        Prefs.backgroundPlay = !Prefs.backgroundPlay
        item.isChecked = Prefs.backgroundPlay
        Toast.makeText(
            this,
            if (Prefs.backgroundPlay) R.string.background_play_on else R.string.background_play_off,
            Toast.LENGTH_LONG
        ).show()
        if (!Prefs.backgroundPlay) {
            stopService(Intent(this, PlaybackService::class.java))
        }
    }

    private fun toggleDesktopMode(item: MenuItem) {
        Prefs.desktopMode = !Prefs.desktopMode
        item.isChecked = Prefs.desktopMode
        for (tab in TabManager.tabs) {
            WebViewFactory.applyUserAgent(tab.webView)
        }
        TabManager.current()?.webView?.reload()
    }

    private fun showShieldInfo() {
        val s = AdBlockEngine.stats
        val source = AdBlockEngine.updatedOn(this) ?: s.source
        AlertDialog.Builder(this)
            .setTitle(R.string.shield_info_title)
            .setMessage(
                getString(
                    R.string.shield_info_msg,
                    AdBlockEngine.blockedThisPage,
                    AdBlockEngine.blockedSession,
                    Prefs.blockedTotal,
                    source,
                    s.rules + s.exceptions,
                    s.domains
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // =========================================================================
    // Permissions & shield status
    // =========================================================================

    private fun maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && Prefs.backgroundPlay) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun watchShieldReady() {
        var attempts = 0
        val runnable = object : Runnable {
            override fun run() {
                if (shieldToastShown) return
                if (AdBlockEngine.ready) {
                    shieldToastShown = true
                    val s = AdBlockEngine.stats
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.shield_active_toast, s.domains, s.rules + s.exceptions),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                attempts++
                if (attempts < 20) handler.postDelayed(this, 1500)
            }
        }
        handler.postDelayed(runnable, 1200)
    }
}

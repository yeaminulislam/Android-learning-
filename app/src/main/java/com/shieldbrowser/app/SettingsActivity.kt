package com.shieldbrowser.app

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.shieldbrowser.app.adblock.AdBlockEngine
import com.shieldbrowser.app.adblock.FilterUpdater
import com.shieldbrowser.app.browser.TabManager
import com.shieldbrowser.app.browser.WebViewFactory
import com.shieldbrowser.app.store.HistoryStore

/** Settings screen: protection, general, filters, about. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        bindSwitch(R.id.switchAdBlock, Prefs.adBlockEnabled) { Prefs.adBlockEnabled = it }
        bindSwitch(R.id.switchCosmetic, Prefs.cosmeticEnabled) { Prefs.cosmeticEnabled = it }
        bindSwitch(R.id.switchPopups, Prefs.blockPopups) { Prefs.blockPopups = it }
        bindSwitch(R.id.switchYtSkip, Prefs.youtubeAdSkip) { Prefs.youtubeAdSkip = it }
        bindSwitch(R.id.switchBackground, Prefs.backgroundPlay) { Prefs.backgroundPlay = it }
        bindSwitch(R.id.switchDesktop, Prefs.desktopMode) { value ->
            Prefs.desktopMode = value
            for (tab in TabManager.tabs) {
                WebViewFactory.applyUserAgent(tab.webView)
            }
        }

        bindSearchEngine()
        bindFilterSection()
        bindAbout()
    }

    private fun bindSwitch(id: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        val sw: MaterialSwitch = findViewById(id)
        sw.isChecked = initial
        sw.setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
    }

    private fun bindSearchEngine() {
        val group: RadioGroup = findViewById(R.id.radioSearch)
        val checkedId = when (Prefs.searchEngine) {
            "duckduckgo" -> R.id.radioDuckDuckGo
            "bing" -> R.id.radioBing
            "brave" -> R.id.radioBrave
            else -> R.id.radioGoogle
        }
        group.check(checkedId)
        group.setOnCheckedChangeListener { _, id ->
            Prefs.searchEngine = when (id) {
                R.id.radioDuckDuckGo -> "duckduckgo"
                R.id.radioBing -> "bing"
                R.id.radioBrave -> "brave"
                else -> "google"
            }
        }
    }

    private fun bindFilterSection() {
        val btn: MaterialButton = findViewById(R.id.btnUpdateFilters)
        val status: TextView = findViewById(R.id.filterStatus)
        refreshFilterStatus(status)

        btn.setOnClickListener {
            btn.isEnabled = false
            Toast.makeText(this, R.string.updating_filters, Toast.LENGTH_SHORT).show()
            FilterUpdater.update(this, object : FilterUpdater.Callback {
                override fun onResult(success: Boolean, message: String) {
                    btn.isEnabled = true
                    Toast.makeText(
                        this@SettingsActivity,
                        if (success) getString(R.string.filters_updated) + "\n" + message
                        else getString(R.string.filters_update_failed),
                        Toast.LENGTH_LONG
                    ).show()
                    refreshFilterStatus(status)
                }
            })
        }

        val clearBtn: MaterialButton = findViewById(R.id.btnClearHistory)
        clearBtn.setOnClickListener {
            HistoryStore.clear()
            Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshFilterStatus(status: TextView) {
        val s = AdBlockEngine.stats
        val source = AdBlockEngine.updatedOn(this) ?: s.source
        status.text = "🛡️ ${s.domains} domains · ${s.rules + s.exceptions} rules · " +
                "${s.cosmetic} cosmetic · $source"
    }

    private fun bindAbout() {
        val version: TextView = findViewById(R.id.versionText)
        version.text = "${getString(R.string.prefs_version)} ${BuildConfig.VERSION_NAME}\n" +
                "Engine: Android System WebView"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

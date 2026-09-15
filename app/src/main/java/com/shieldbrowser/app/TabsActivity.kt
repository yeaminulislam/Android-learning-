package com.shieldbrowser.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.shieldbrowser.app.browser.TabManager

/** Tab switcher screen. */
class TabsActivity : AppCompatActivity() {

    private lateinit var adapter: TabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.tabs_title)

        val recycler: RecyclerView = findViewById(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = TabsAdapter()
        recycler.adapter = adapter

        val fab: ExtendedFloatingActionButton = findViewById(R.id.fabAddTab)
        fab.setOnClickListener {
            val main = MainActivity.instance?.get()
            if (main == null) {
                Toast.makeText(this, R.string.cant_open_link, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            TabManager.createTab(main, BrowserApp.HOME_URL)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ----------------------------------------------------------------------

    inner class TabsAdapter : RecyclerView.Adapter<TabsAdapter.TabVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tab, parent, false)
            return TabVH(view)
        }

        override fun getItemCount(): Int = TabManager.tabs.size

        override fun onBindViewHolder(holder: TabVH, position: Int) {
            val tab = TabManager.tabs[position]
            holder.title.text = tab.title.ifBlank { tab.url ?: getString(R.string.new_tab) }
            holder.url.text = tab.url ?: ""

            val isCurrent = position == TabManager.currentPosition()
            holder.itemView.alpha = if (isCurrent) 1.0f else 0.75f

            holder.itemView.setOnClickListener {
                TabManager.select(holder.bindingAdapterPosition)
                finish()
            }
            holder.close.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos in TabManager.tabs.indices) {
                    TabManager.closeTab(pos)
                    if (TabManager.tabs.isEmpty()) {
                        val main = MainActivity.instance?.get()
                        if (main != null) {
                            TabManager.createTab(main, BrowserApp.HOME_URL)
                        }
                    }
                    notifyDataSetChanged()
                    if (TabManager.tabs.isEmpty()) finish()
                }
            }
        }

        inner class TabVH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tabTitle)
            val url: TextView = view.findViewById(R.id.tabUrl)
            val close: ImageButton = view.findViewById(R.id.btnClose)
        }
    }
}

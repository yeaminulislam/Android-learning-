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
import com.google.android.material.materialswitch.MaterialSwitch
import com.shieldbrowser.app.ext.ExtensionStore
import com.shieldbrowser.app.ext.Userscript
import com.shieldbrowser.app.ext.UserscriptEngine

/** Userscript/extension manager: enable, disable, delete, find more. */
class ExtensionsActivity : AppCompatActivity() {

    private lateinit var adapter: ExtAdapter
    private val items = ArrayList<Userscript>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extensions)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.extensions_title)

        val recycler: RecyclerView = findViewById(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ExtAdapter()
        recycler.adapter = adapter

        val fab: ExtendedFloatingActionButton = findViewById(R.id.fabAdd)
        fab.setOnClickListener {
            // Greasyfork is the biggest userscript library; .user.js links are
            // intercepted by the browser and installed from there directly.
            PendingNavigate.url = "https://greasyfork.org/scripts"
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        items.clear()
        items.addAll(ExtensionStore.all())
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.emptyText).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    inner class ExtAdapter : RecyclerView.Adapter<ExtAdapter.ExtVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_extension, parent, false)
            return ExtVH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ExtVH, position: Int) {
            val script = items[position]
            holder.name.text = "${script.name}  v${script.version}"
            val info = buildString {
                if (script.description.isNotBlank()) {
                    append(script.description).append('\n')
                }
                append(getString(R.string.script_matches_label)).append(' ')
                append(script.includes.take(3).joinToString(", "))
            }
            holder.info.text = info

            holder.sw.setOnCheckedChangeListener(null)
            holder.sw.isChecked = script.enabled
            holder.sw.setOnCheckedChangeListener { _, checked ->
                script.enabled = checked
                ExtensionStore.setEnabled(script.id, checked)
                UserscriptEngine.invalidate()
            }

            holder.delete.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos in items.indices) {
                    val s = items[pos]
                    ExtensionStore.remove(s.id)
                    UserscriptEngine.invalidate()
                    Toast.makeText(
                        this@ExtensionsActivity,
                        R.string.script_removed, Toast.LENGTH_SHORT
                    ).show()
                    reload()
                }
            }
        }

        inner class ExtVH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.extName)
            val info: TextView = view.findViewById(R.id.extInfo)
            val sw: MaterialSwitch = view.findViewById(R.id.extSwitch)
            val delete: ImageButton = view.findViewById(R.id.btnDelete)
        }
    }
}

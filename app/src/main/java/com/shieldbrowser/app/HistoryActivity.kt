package com.shieldbrowser.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shieldbrowser.app.store.HistoryStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** History list screen (newest first). */
class HistoryActivity : AppCompatActivity() {

    private lateinit var adapter: SimpleRowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.history)

        val recycler: RecyclerView = findViewById(R.id.recycler)
        val empty: TextView = findViewById(R.id.emptyText)
        empty.setText(R.string.empty_history)

        adapter = SimpleRowAdapter(
            onClick = { pos ->
                val entries = HistoryStore.all()
                if (pos in entries.indices) {
                    PendingNavigate.url = entries[pos].url
                    finish()
                }
            },
            onDelete = { pos ->
                HistoryStore.removeAt(pos)
                refresh(empty)
            }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        refresh(empty)
    }

    private fun refresh(empty: TextView) {
        val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val rows = HistoryStore.all().map {
            SimpleRowAdapter.Row(
                it.title.ifBlank { it.url },
                fmt.format(Date(it.time)) + "  ·  " + it.url
            )
        }
        adapter.setData(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

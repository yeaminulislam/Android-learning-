package com.shieldbrowser.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shieldbrowser.app.store.BookmarkStore

/** Bookmark list screen. Tap = open in new tab, X = delete. */
class BookmarksActivity : AppCompatActivity() {

    private lateinit var adapter: SimpleRowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.bookmarks)

        val recycler: RecyclerView = findViewById(R.id.recycler)
        val empty: TextView = findViewById(R.id.emptyText)
        empty.setText(R.string.empty_bookmarks)

        adapter = SimpleRowAdapter(
            onClick = { pos ->
                val bookmarks = BookmarkStore.all()
                if (pos in bookmarks.indices) {
                    PendingNavigate.url = bookmarks[pos].url
                    finish()
                }
            },
            onDelete = { pos ->
                val bookmarks = BookmarkStore.all()
                if (pos in bookmarks.indices) {
                    BookmarkStore.remove(bookmarks[pos].url)
                    refresh(empty)
                }
            }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        refresh(empty)
    }

    private fun refresh(empty: TextView) {
        val rows = BookmarkStore.all().map { SimpleRowAdapter.Row(it.title, it.url) }
        adapter.setData(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

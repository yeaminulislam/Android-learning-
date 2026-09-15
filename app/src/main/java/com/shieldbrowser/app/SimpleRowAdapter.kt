package com.shieldbrowser.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Generic two-line row adapter used by Bookmarks & History screens. */
class SimpleRowAdapter(
    private val onClick: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<SimpleRowAdapter.VH>() {

    data class Row(val title: String, val sub: String)

    private val rows = ArrayList<Row>()

    fun setData(newRows: List<Row>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    fun isEmpty(): Boolean = rows.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.title.text = row.title.ifBlank { row.sub }
        holder.sub.text = row.sub
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
        holder.delete.setOnClickListener { onDelete(holder.bindingAdapterPosition) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.rowTitle)
        val sub: TextView = view.findViewById(R.id.rowSub)
        val delete: ImageButton = view.findViewById(R.id.btnDelete)
    }
}

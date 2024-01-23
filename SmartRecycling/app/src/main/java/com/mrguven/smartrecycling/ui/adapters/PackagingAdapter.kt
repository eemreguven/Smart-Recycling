package com.mrguven.smartrecycling.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mrguven.smartrecycling.R
import com.mrguven.smartrecycling.data.model.Packaging

class PackagingAdapter(private val items: List<Packaging>) : RecyclerView.Adapter<PackagingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val countTextView: TextView = itemView.findViewById(R.id.packagingItemCount)
        val textViewItem: TextView = itemView.findViewById(R.id.packagingItemTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_packaging_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.countTextView.text = item.count.toString()
        holder.textViewItem.text = item.name
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
package com.mrguven.smartrecycling.ui.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mrguven.smartrecycling.R
import com.mrguven.smartrecycling.data.local.RecycledPackaging
import com.mrguven.smartrecycling.data.model.Packaging

class RecycledPackagingAdapter(private val items: List<RecycledPackaging>) : RecyclerView.Adapter<RecycledPackagingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val countTextView: TextView = itemView.findViewById(R.id.packagingItemCount)
        val textViewItem: TextView = itemView.findViewById(R.id.packagingItemTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycled_packaging_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.countTextView.text = item.count.toString()
        holder.textViewItem.text = item.title


        val colorResId = when (item.type) {
            "GLASS" -> R.color.glass_color
            "PLASTIC" -> R.color.plastic_color
            else -> R.color.metal_color
        }

        val backgroundColor = ContextCompat.getColor(holder.itemView.context, colorResId)
        holder.itemView.setBackgroundColor(backgroundColor)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
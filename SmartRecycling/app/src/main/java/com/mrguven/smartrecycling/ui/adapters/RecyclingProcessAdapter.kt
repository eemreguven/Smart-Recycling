package com.mrguven.smartrecycling.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrguven.smartrecycling.R
import com.mrguven.smartrecycling.data.local.RecycledPackaging
import com.mrguven.smartrecycling.data.local.RecyclingProcessWithPackaging
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecyclingProcessAdapter(
    private var recyclingProcesses: List<RecyclingProcessWithPackaging>,
    private val onItemClick: (RecyclingProcessWithPackaging) -> Unit
) : RecyclerView.Adapter<RecyclingProcessAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.packagingListRecyclerView)

        fun bind(recyclingProcess: RecyclingProcessWithPackaging) {
            itemView.findViewById<TextView>(R.id.textViewProcessName).text = recyclingProcess.recyclingProcess.containerName
            itemView.findViewById<TextView>(R.id.textViewProcessDate).text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(
                Date(recyclingProcess.recyclingProcess.date)
            )
            itemView.setOnClickListener {

            }
            itemView.setOnClickListener {
                if (recyclerView.visibility == View.VISIBLE) {
                    recyclerView.visibility = View.GONE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    showRecycledPackagingDialog(recyclingProcess.recycledPackagingList)
                }
            }
        }
        private fun showRecycledPackagingDialog(recycledPackagingList: List<RecycledPackaging>) {
            val recycledPackagingAdapter = RecycledPackagingAdapter(recycledPackagingList)
            recyclerView.layoutManager = LinearLayoutManager(itemView.context)
            recyclerView.adapter = recycledPackagingAdapter
        }

    }

    fun updateData(newData: List<RecyclingProcessWithPackaging>) {
        val diffResult = DiffUtil.calculateDiff(RecyclingProcessDiffCallback(recyclingProcesses, newData))
        recyclingProcesses = newData
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycling_process, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(recyclingProcesses[position])
    }

    override fun getItemCount(): Int {
        return recyclingProcesses.size
    }

    class RecyclingProcessDiffCallback(
        private val oldList: List<RecyclingProcessWithPackaging>,
        private val newList: List<RecyclingProcessWithPackaging>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].recyclingProcess.id == newList[newItemPosition].recyclingProcess.id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

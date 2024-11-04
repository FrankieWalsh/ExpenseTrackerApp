package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R

class GroupAdapter(private val groups: List<String>) :
    RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(R.id.textViewGroupName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.groupName.text = groups[position]
        holder.itemView.setOnClickListener {
            // Handle item click: navigate to group detail screen or show group info
        }
    }

    override fun getItemCount(): Int = groups.size
}

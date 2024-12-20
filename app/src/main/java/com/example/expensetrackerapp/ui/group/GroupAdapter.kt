package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Group

// GroupAdapter class, either as a standalone or inner class
class GroupAdapter(
    private val groups: List<Group>,
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(R.id.textViewGroupName)
        val groupDescription: TextView = itemView.findViewById(R.id.textViewGroupDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.groupName.text = group.name
        holder.groupDescription.text = group.description

        val context = holder.itemView.context
        val backgroundColor = context.getColor(R.color.group_item_background)
        val textColor = context.getColor(R.color.textColorPrimary) // Assuming white text for dark mode

        holder.itemView.setBackgroundColor(backgroundColor)
        holder.groupName.setTextColor(textColor)
        holder.groupDescription.setTextColor(textColor)
        // Make the entire item view clickable
        holder.itemView.setOnClickListener {
            onGroupClick(group)
        }
    }


    override fun getItemCount(): Int = groups.size
}

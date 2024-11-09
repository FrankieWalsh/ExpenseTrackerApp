package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Group
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class GroupAdapter(
    private val groups: List<Group>,
    private val onClick: (Group) -> Unit,
    private val onJoinClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(R.id.textViewGroupName)
        val groupDescription: TextView = itemView.findViewById(R.id.textViewGroupDescription)
        val cardView: MaterialCardView = itemView as MaterialCardView
        val joinButton: MaterialButton = itemView.findViewById(R.id.buttonJoinGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.groupName.text = group.name
        holder.groupDescription.text = group.description

        holder.cardView.setOnClickListener {
            onClick(group)
        }

        holder.joinButton.setOnClickListener {
            onJoinClick(group)
        }
    }

    override fun getItemCount(): Int = groups.size
}

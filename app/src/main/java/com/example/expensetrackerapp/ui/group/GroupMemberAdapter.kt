package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.databinding.ItemGroupMemberBinding
import com.example.expensetrackerapp.model.User

class GroupMemberAdapter(private val members: List<User>) :
    RecyclerView.Adapter<GroupMemberAdapter.GroupMemberViewHolder>() {

    inner class GroupMemberViewHolder(val binding: ItemGroupMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int) {
        val member = members[position]
        holder.binding.textViewMemberName.text = member.name ?: member.email
    }

    override fun getItemCount(): Int = members.size
}
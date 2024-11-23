package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.GroupMemberWithBalance
import com.example.expensetrackerapp.model.User


class GroupMembersAdapter(
    private val members: List<GroupMemberWithBalance>
) : RecyclerView.Adapter<GroupMembersAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member)
    }

    override fun getItemCount(): Int = members.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memberNameTextView: TextView = itemView.findViewById(R.id.textViewMemberName)
        private val balanceTextView: TextView = itemView.findViewById(R.id.textViewBalance)

        fun bind(member: GroupMemberWithBalance) {
            memberNameTextView.text = member.name
            balanceTextView.text = "$${member.balance}"

            when {
                member.balance > 0 -> balanceTextView.setTextColor(itemView.context.getColor(R.color.green_40))
                member.balance < 0 -> balanceTextView.setTextColor(itemView.context.getColor(R.color.delete_color))
                else -> balanceTextView.setTextColor(itemView.context.getColor(R.color.grey))
            }
        }
    }
}

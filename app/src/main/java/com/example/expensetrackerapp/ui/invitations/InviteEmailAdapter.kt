package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R

class InviteEmailAdapter(private val emails: List<String>) : RecyclerView.Adapter<InviteEmailAdapter.InviteEmailViewHolder>() {

    inner class InviteEmailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailTextView: TextView = itemView.findViewById(R.id.textViewInviteEmail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteEmailViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_invite_email, parent, false)
        return InviteEmailViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteEmailViewHolder, position: Int) {
        holder.emailTextView.text = emails[position]
    }

    override fun getItemCount(): Int = emails.size
}

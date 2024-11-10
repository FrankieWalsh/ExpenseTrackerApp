package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Invitation

class InvitationAdapter(
    private val invitations: List<Invitation>,
    private val onAccept: (Invitation) -> Unit,
    private val onDecline: (Invitation) -> Unit
) : RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder>() {

    inner class InvitationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(R.id.textViewGroupName)
        val acceptButton: Button = itemView.findViewById(R.id.buttonAcceptInvitation)
        val declineButton: Button = itemView.findViewById(R.id.buttonDeclineInvitation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_invitation, parent, false)
        return InvitationViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        val invitation = invitations[position]
        holder.groupName.text = "Group ID: ${invitation.groupId}"

        holder.acceptButton.setOnClickListener {
            onAccept(invitation)
        }

        holder.declineButton.setOnClickListener {
            onDecline(invitation)
        }
    }

    override fun getItemCount(): Int = invitations.size
}

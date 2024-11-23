package com.example.expensetrackerapp.ui.expense


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R

class ParticipantAdapter (private val participants: List<String>) :
    RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder>() {

    //viewHolder para cada participante
    inner class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameTextView: TextView = itemView.findViewById(R.id.ViewParticipants)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        //asignar el nombre del participante a la vista correspondiente
        holder.nameTextView.text = participants[position]
    }

    override fun getItemCount(): Int = participants.size
}
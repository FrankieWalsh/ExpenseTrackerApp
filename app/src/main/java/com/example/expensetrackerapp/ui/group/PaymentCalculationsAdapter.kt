package com.example.expensetrackerapp.ui.group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.GroupMemberWithBalance

class PaymentCalculationsAdapter(
    private val members: List<GroupMemberWithBalance>,
    private val onPayClick: (GroupMemberWithBalance) -> Unit
) : RecyclerView.Adapter<PaymentCalculationsAdapter.PaymentCalculationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentCalculationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_calculation, parent, false)
        return PaymentCalculationViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentCalculationViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member, onPayClick)
    }

    override fun getItemCount(): Int = members.size

    class PaymentCalculationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val phraseTextView: TextView = itemView.findViewById(R.id.textViewPhrase)
        private val payButton: Button = itemView.findViewById(R.id.buttonPayMember)

        fun bind(member: GroupMemberWithBalance, onPayClick: (GroupMemberWithBalance) -> Unit) {
            val balance = member.balance
            if (balance < 0) {
                // You owe money to the member
                val amountOwed = String.format("%.2f", -balance)
                phraseTextView.text = "You owe $$amountOwed to ${member.name}"
                payButton.visibility = View.VISIBLE
                payButton.setOnClickListener {
                    onPayClick(member)
                }
            } else if (balance > 0) {
                // Member owes money to you
                val amountOwed = String.format("%.2f", balance)
                phraseTextView.text = "${member.name} owes you $$amountOwed"
                payButton.visibility = View.GONE
            } else {
                // Balance is zero
                phraseTextView.text = "You are settled up with ${member.name}"
                payButton.visibility = View.GONE
            }
        }
    }
}

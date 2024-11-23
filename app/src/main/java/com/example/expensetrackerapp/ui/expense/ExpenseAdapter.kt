package com.example.expensetrackerapp.ui.expense

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onPayClick: (Expense) -> Unit // Callback for "Pay" button click
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewExpenseDescription)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewExpenseCategory)
        val amountTextView: TextView = itemView.findViewById(R.id.textViewExpenseAmount)
        val participantsView: RecyclerView = itemView.findViewById(R.id.ViewParticipants)
        val payButton: Button = itemView.findViewById(R.id.buttonPay)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }
    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.descriptionTextView.text = expense.description
        holder.categoryTextView.text = expense.category
       // holder.participantsView.recycler = expense.participant

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        val participantAdapter = ParticipantAdapter(expense.participant)
        holder.participantsView.apply {
            layoutManager = LinearLayoutManager(holder.itemView.context)
            adapter = participantAdapter
        }

        if (expense.payerId == currentUserId) {
            // If the current user is the payer, show the total amount paid
            firestore.collection("expense_splits")
                .whereEqualTo("expenseId", expense.id)
                .whereEqualTo("hasPaid", true)
                .get()
                .addOnSuccessListener { result ->
                    val totalPaid = result.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                    holder.amountTextView.text = "Amount: $${expense.amount} (Amount recieved: $${totalPaid})"
                    holder.payButton.visibility = View.GONE // Hide pay button for payer
                }
        } else {
            // If the current user is not the payer, show their split and check if they have paid
            firestore.collection("expense_splits")
                .whereEqualTo("expenseId", expense.id)
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener { result ->
                    val splitDocument = result.documents.firstOrNull()
                    val splitAmount = splitDocument?.getDouble("amount") ?: 0.0
                    val hasPaid = splitDocument?.getBoolean("hasPaid") ?: false

                    // Show "Paid!" next to the split amount if the user has paid
                    holder.amountTextView.text = if (hasPaid) {
                        "Your split: $${splitAmount} (Paid!)"
                    } else {
                        "Your split: $${splitAmount}"
                    }

                    if (hasPaid) {
                        // If the user has already paid, disable the button and show "Paid"
                        holder.payButton.text = "Paid"
                        holder.payButton.isEnabled = false
                        holder.payButton.alpha = 0.5f // Grey out the button
                    } else {
                        // If the user hasn't paid, enable the button and set it to "Pay"
                        holder.payButton.text = "Pay"
                        holder.payButton.isEnabled = true
                        holder.payButton.alpha = 1.0f // Normal appearance
                        holder.payButton.visibility = View.VISIBLE // Ensure button is visible
                        holder.payButton.setOnClickListener {
                            onPayClick(expense)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    holder.amountTextView.text = "Your split: (Error fetching split)"
                    holder.payButton.visibility = View.GONE // Hide the button if there's an error
                }
        }
    }



    override fun getItemCount(): Int = expenses.size
}


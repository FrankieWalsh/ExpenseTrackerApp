package com.example.expensetrackerapp.ui.expense

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExpenseAdapter(
    private var expenses: List<Expense>
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewExpenseDescription)
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewExpenseCategory)
        val amountTextView: TextView = itemView.findViewById(R.id.textViewExpenseAmount)
        val paidByTextView: TextView = itemView.findViewById(R.id.textViewPaidBy)
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
        holder.amountTextView.text = "$${String.format("%.2f", expense.amount)}"

        firestore.collection("users").document(expense.payerId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val payerName = document.getString("name") ?: "Unknown"
                    holder.paidByTextView.text = "Paid by: $payerName"
                } else {
                    holder.paidByTextView.text = "Paid by: Unknown"
                }
            }
            .addOnFailureListener {
                holder.paidByTextView.text = "Paid by: Unknown"
            }

    }

    override fun getItemCount(): Int = expenses.size
}

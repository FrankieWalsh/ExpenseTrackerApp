package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.ExpenseSplit
import com.example.expensetrackerapp.model.GroupMemberWithBalance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SummaryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var groupId: String? = null

    private lateinit var totalOwedTextView: TextView
    private lateinit var totalOwingTextView: TextView
    private lateinit var itemsPaidTextView: TextView
    private lateinit var paymentRecyclerView: RecyclerView
    private lateinit var balancesRecyclerView: RecyclerView

    private val userNamesCache = mutableMapOf<String, String>() // Cache for user names

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_summary, container, false)
        firestore = FirebaseFirestore.getInstance()
        groupId = arguments?.getString("groupId")

        totalOwedTextView = view.findViewById(R.id.textViewTotalOwed)
        totalOwingTextView = view.findViewById(R.id.textViewTotalOwing)
        itemsPaidTextView = view.findViewById(R.id.textViewItemsPaid)
        balancesRecyclerView = view.findViewById(R.id.recyclerViewGroupMembers)
        paymentRecyclerView = view.findViewById(R.id.recyclerViewPaymentCalculations)

        calculateSummary()
        fetchGroupMembersWithBalances()

        return view
    }

    private fun calculateSummary() {
        var totalOwed = 0.0
        var totalOwing = 0.0
        var itemsPaidCount = 0

        groupId?.let { groupId ->
            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { result ->
                    totalOwing = result.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                    totalOwingTextView.text = "Total Owing: $${String.format("%.2f", totalOwing)}"
                }

            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("owedTo", currentUserId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { result ->
                    totalOwed = result.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                    totalOwedTextView.text = "Total Owed: $${String.format("%.2f", totalOwed)}"
                }

            firestore.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("payerId", currentUserId)
                .get()
                .addOnSuccessListener { result ->
                    itemsPaidCount = result.size()
                    itemsPaidTextView.text = "Expenses Added: $itemsPaidCount"
                }
        }
    }

    private fun fetchGroupMembersWithBalances() {
        groupId?.let { groupId ->
            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { splitsResult ->
                    val splits = splitsResult.documents.mapNotNull { it.toObject(ExpenseSplit::class.java) }
                    calculateBalancesAndFetchNames(splits)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error fetching splits", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun calculateBalancesAndFetchNames(splits: List<ExpenseSplit>) {
        val balances = mutableMapOf<String, Double>()

        for (split in splits) {
            balances[split.userId] = (balances[split.userId] ?: 0.0) - split.amount
            balances[split.owedTo] = (balances[split.owedTo] ?: 0.0) + split.amount
        }

        val memberIds = balances.keys.toList()
        fetchUserNames(memberIds) { userNames ->
            val membersWithBalances = balances.map { (userId, balance) ->
                GroupMemberWithBalance(userId, userNames[userId] ?: "Unknown", balance)
            }

            val userPayments = membersWithBalances.filter { it.id != currentUserId }

            // Update adapters
            val balancesAdapter = GroupMembersAdapter(membersWithBalances)
            balancesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            balancesRecyclerView.adapter = balancesAdapter

            val paymentAdapter = PaymentCalculationsAdapter(userPayments) { member ->
                showPayDialog(member)
            }
            paymentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            paymentRecyclerView.adapter = paymentAdapter
        }
    }

    private fun fetchUserNames(userIds: List<String>, callback: (Map<String, String>) -> Unit) {
        val missingIds = userIds.filterNot { userNamesCache.containsKey(it) }

        if (missingIds.isEmpty()) {
            callback(userNamesCache)
            return
        }

        firestore.collection("users")
            .whereIn("id", missingIds)
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    val id = document.getString("id") ?: continue
                    val name = document.getString("name") ?: "Unknown"
                    userNamesCache[id] = name
                }
                callback(userNamesCache)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error fetching user names", Toast.LENGTH_SHORT).show()
                callback(userNamesCache) // Use what we have in cache
            }
    }

    private fun showPayDialog(member: GroupMemberWithBalance) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pay_member, null)
        val amountTextView = dialogView.findViewById<TextView>(R.id.textViewAmount)
        val amountToPay = -member.balance
        amountTextView.text = "Amount to pay: $${String.format("%.2f", amountToPay)}"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pay ${member.name}")
            .setView(dialogView)
            .setPositiveButton("Pay") { _, _ ->
                markSplitsAsPaid(member.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun markSplitsAsPaid(memberId: String) {
        groupId?.let { groupId ->
            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("owedTo", memberId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { result ->
                    val batch = firestore.batch()
                    for (document in result.documents) {
                        val splitRef = document.reference
                        batch.update(splitRef, "hasPaid", true)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Payment marked as complete", Toast.LENGTH_SHORT).show()
                            calculateSummary()
                            fetchGroupMembersWithBalances()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error marking payment: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching splits: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

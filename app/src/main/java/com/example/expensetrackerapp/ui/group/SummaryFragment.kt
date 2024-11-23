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
import com.example.expensetrackerapp.model.GroupMember
import com.example.expensetrackerapp.model.GroupMemberWithBalance
import com.example.expensetrackerapp.model.User
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SummaryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var groupId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_summary, container, false)
        firestore = FirebaseFirestore.getInstance()
        groupId = arguments?.getString("groupId")

        val totalOwedTextView = view.findViewById<TextView>(R.id.textViewTotalOwed)
        val totalOwingTextView = view.findViewById<TextView>(R.id.textViewTotalOwing)
        val itemsPaidTextView = view.findViewById<TextView>(R.id.textViewItemsPaid)

        calculateSummary(totalOwedTextView, totalOwingTextView, itemsPaidTextView)
        fetchGroupMembersWithBalances()

        return view
    }

    private fun calculateSummary(
        totalOwedTextView: TextView,
        totalOwingTextView: TextView,
        itemsPaidTextView: TextView
    ) {
        var totalOwed = 0.0
        var totalOwing = 0.0
        var itemsPaidCount = 0

        groupId?.let { groupId ->
            Log.d(
                "SummaryFragment",
                "Calculating summary for groupId: $groupId and userId: $currentUserId"
            )

            // Fetch expense splits where the user owes money for this group
            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { result ->
                    Log.d(
                        "SummaryFragment",
                        "Total Owing Result: ${result.documents}"
                    )  // Log the whole result
                    for (document in result) {
                        val amount = document.getDouble("amount") ?: 0.0
                        totalOwing += amount
                        Log.d(
                            "SummaryFragment",
                            "Adding to totalOwing: $amount, new totalOwing: $totalOwing"
                        )
                    }
                    totalOwingTextView.text = "Total Owing: $${totalOwing}"
                    Log.d("SummaryFragment", "Final totalOwing: $totalOwing")
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching owing data: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("SummaryFragment", "Error fetching owing data: ${it.message}")
                }

            // Fetch expense splits where the user is owed money for this group
            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("owedTo", currentUserId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { result ->
                    Log.d(
                        "SummaryFragment",
                        "Total Owed Result: ${result.documents}"
                    )  // Log the whole result
                    for (document in result) {
                        val amount = document.getDouble("amount") ?: 0.0
                        totalOwed += amount
                        Log.d(
                            "SummaryFragment",
                            "Adding to totalOwed: $amount, new totalOwed: $totalOwed"
                        )
                    }
                    totalOwedTextView.text = "Total Owed: $${totalOwed}"
                    Log.d("SummaryFragment", "Final totalOwed: $totalOwed")
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching owed data: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("SummaryFragment", "Error fetching owed data: ${it.message}")
                }

            // Count the items the user has paid for in this group
            firestore.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("payerId", currentUserId)
                .get()
                .addOnSuccessListener { result ->
                    Log.d(
                        "SummaryFragment",
                        "Items Paid For Result: ${result.documents}"
                    )  // Log the whole result
                    itemsPaidCount = result.size()
                    itemsPaidTextView.text = "Expenses Added: $itemsPaidCount"
                    Log.d("SummaryFragment", "Total items paid for: $itemsPaidCount")
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching paid items: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("SummaryFragment", "Error fetching paid items: ${it.message}")
                }
        } ?: Log.e("SummaryFragment", "GroupId is null")
    }


    private fun fetchGroupMembersWithBalances() {
        groupId?.let { groupId ->
            // obtain the members of a group
            firestore.collection("group_members")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { groupMembersResult ->
                    val memberIds =
                        groupMembersResult.documents.map { it.getString("userId") ?: "" }

                    // details of each user
                    firestore.collection("users")
                        .whereIn("id", memberIds)
                        .get()
                        .addOnSuccessListener { userResult ->
                            val members = userResult.documents.map { doc ->
                                User(
                                    id = doc.getString("id") ?: "",
                                    name = doc.getString("name") ?: "Unknown"
                                )
                            }

                            // obtain the balance of the members
                            val membersWithBalance = mutableListOf<GroupMemberWithBalance>()
                            val tasks =
                                mutableListOf<Task<Void>>()  // To wait for the requests

                            //Go through each member and calculate the balance
                            members.forEach { member ->
                                val task = calculateBalanceForMember(member.id) { balance ->
                                    membersWithBalance.add(
                                        GroupMemberWithBalance(
                                            member.id,
                                            member.name,
                                            balance
                                        )
                                    )

                                    // Update the RecyclerView
                                    if (membersWithBalance.size == members.size) {
                                        val recyclerView =
                                            view?.findViewById<RecyclerView>(R.id.recyclerViewGroupMembers)
                                        recyclerView?.layoutManager =
                                            LinearLayoutManager(requireContext())
                                        val adapter = GroupMembersAdapter(membersWithBalance)
                                        recyclerView?.adapter = adapter
                                    }
                                }
                                tasks.add(task)
                            }

                            //Wait to all the task to finish
                            Tasks.whenAll(*tasks.toTypedArray()).addOnCompleteListener {
                                //all the balances have been loaded
                                Log.d(
                                    "SummaryFragment",
                                    "Finished loading all members with balances"
                                )
                            }

                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Error fetching users: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching group members: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }


    private fun calculateBalanceForMember(memberId: String, callback: (Double) -> Unit): Task<Void> {
        val taskCompletionSource = TaskCompletionSource<Void>()
        var balance = 0.0


        firestore.collection("expense_splits")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("hasPaid", false)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val owedAmount = document.getDouble("amount") ?: 0.0
                    if (document.getString("owedTo") == memberId) {
                        balance += owedAmount  // Le deben dinero
                    } else if (document.getString("userId") == memberId) {
                        balance -= owedAmount  // Él/ella debe dinero
                    }
                }

                callback(balance)
                taskCompletionSource.setResult(null)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error calculating balance: ${it.message}", Toast.LENGTH_SHORT).show()
                taskCompletionSource.setException(it)
            }

        return taskCompletionSource.task


    }



}

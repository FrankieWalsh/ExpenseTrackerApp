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
        //loadGroupMembers(view)
        fetchGroupMembersWithBalances()

        return view
    }

    private fun calculateSummary(totalOwedTextView: TextView, totalOwingTextView: TextView, itemsPaidTextView: TextView) {
        var totalOwed = 0.0
        var totalOwing = 0.0
        var itemsPaidCount = 0

        groupId?.let { groupId ->
            Log.d("SummaryFragment", "Calculating summary for groupId: $groupId and userId: $currentUserId")

            // Fetch expense splits where the user owes money for this group
            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { result ->
                    Log.d("SummaryFragment", "Total Owing Result: ${result.documents}")  // Log the whole result
                    for (document in result) {
                        val amount = document.getDouble("amount") ?: 0.0
                        totalOwing += amount
                        Log.d("SummaryFragment", "Adding to totalOwing: $amount, new totalOwing: $totalOwing")
                    }
                    totalOwingTextView.text = "Total Owing: $${totalOwing}"
                    Log.d("SummaryFragment", "Final totalOwing: $totalOwing")
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error fetching owing data: ${it.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SummaryFragment", "Error fetching owing data: ${it.message}")
                }

            // Fetch expense splits where the user is owed money for this group
            firestore.collection("expense_splits")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("owedTo", currentUserId)
                .whereEqualTo("hasPaid", false)
                .get()
                .addOnSuccessListener { result ->
                    Log.d("SummaryFragment", "Total Owed Result: ${result.documents}")  // Log the whole result
                    for (document in result) {
                        val amount = document.getDouble("amount") ?: 0.0
                        totalOwed += amount
                        Log.d("SummaryFragment", "Adding to totalOwed: $amount, new totalOwed: $totalOwed")
                    }
                    totalOwedTextView.text = "Total Owed: $${totalOwed}"
                    Log.d("SummaryFragment", "Final totalOwed: $totalOwed")
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error fetching owed data: ${it.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SummaryFragment", "Error fetching owed data: ${it.message}")
                }

            // Count the items the user has paid for in this group
            firestore.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("payerId", currentUserId)
                .get()
                .addOnSuccessListener { result ->
                    Log.d("SummaryFragment", "Items Paid For Result: ${result.documents}")  // Log the whole result
                    itemsPaidCount = result.size()
                    itemsPaidTextView.text = "Expenses Added: $itemsPaidCount"
                    Log.d("SummaryFragment", "Total items paid for: $itemsPaidCount")
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error fetching paid items: ${it.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SummaryFragment", "Error fetching paid items: ${it.message}")
                }
        } ?: Log.e("SummaryFragment", "GroupId is null")
    }
/*
    private fun loadGroupMembers(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGroupMembers)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        groupId?.let { groupId ->
            firestore.collection("group_members")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { result ->
                    val memberIds = result.documents.map { it.getString("userId") ?: "" }

                    // Fetch user details for each member
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
                            //val adapter = GroupMembersAdapter(members)
                            val adapter = GroupMembersAdapter(members)
                            recyclerView.adapter = adapter
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
    }*/
    /*
    private fun fetchGroupMembersWithBalances() {
    groupId?.let { groupId ->
        // Paso 1: Obtén la lista de miembros del grupo
        firestore.collection("group_members")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { membersResult ->
                val members = membersResult.toObjects(GroupMember::class.java)
                val userIds = members.map { it.userId }

                // Paso 2: Obtén los datos de los usuarios (incluyendo el nombre) desde la colección "users"
                firestore.collection("users")
                    .whereIn("userId", userIds) // Suponiendo que "userId" es el campo que identifica al usuario
                    .get()
                    .addOnSuccessListener { usersResult ->
                        // Creamos un mapa con userId como clave y name como valor
                        val userMap = usersResult.documents.associate { doc ->
                            val userId = doc.getString("userId") ?: ""
                            val userName = doc.getString("name") ?: "Desconocido" // Si no tiene nombre, asignamos "Desconocido"
                            userId to userName
                        }

                        // Paso 3: Obtén los balances de cada miembro
                        firestore.collection("expense_splits")
                            .whereEqualTo("groupId", groupId)
                            .get()
                            .addOnSuccessListener { balancesResult ->
                                // Creamos un mapa con userId como clave y balance como valor
                                val balanceMap = balancesResult.documents.associate { doc ->
                                    val userId = doc.getString("userId") ?: ""
                                    val balance = doc.getDouble("balance") ?: 0.0
                                    userId to balance
                                }

                                // Paso 4: Combina la información de los usuarios y los balances
                                val membersWithBalances = members.map { member ->
                                    GroupMemberWithBalance(
                                        name = userMap[member.userId] ?: "Desconocido", // Obtenemos el nombre del mapa de usuarios
                                        balance = balanceMap[member.userId] ?: 0.0 // Obtenemos el balance del mapa de balances
                                    )
                                }

                                // Paso 5: Actualiza el RecyclerView con los miembros y balances
                                setupRecyclerView(membersWithBalances)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error al obtener balances: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al obtener usuarios: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al obtener miembros del grupo: ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }
}
    private fun setupRecyclerView(membersWithBalances: List<GroupMemberWithBalance>) {
        val adapter = GroupMembersAdapter(membersWithBalances)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewGroupMembers)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = adapter
    }

     */

    private fun fetchGroupMembersWithBalances() {
        groupId?.let { groupId ->
            // Obtener los miembros del grupo
            firestore.collection("group_members")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { groupMembersResult ->
                    val memberIds = groupMembersResult.documents.map { it.getString("userId") ?: "" }

                    // Obtener los detalles de cada usuario
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

                            // Obtener los balances de los miembros
                            val membersWithBalance = members.map { member ->
                                val balance = calculateBalanceForMember(member.id)
                                GroupMemberWithBalance(member.id, member.name, balance)
                            }

                            // Aquí puedes ahora cargar la lista de miembros con su balance en un RecyclerView
                            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerViewGroupMembers)
                            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
                            val adapter = GroupMembersAdapter(membersWithBalance)
                            recyclerView?.adapter = adapter
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error fetching users: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error fetching group members: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun calculateBalanceForMember(memberId: String): Double {
        var balance = 0.0

        firestore.collection("expense_splits")
            .whereEqualTo("userId", memberId)
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
            }
        return balance
    }
}

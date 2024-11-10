package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.expensetrackerapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupOverviewFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var groupId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_overview, container, false)
        firestore = FirebaseFirestore.getInstance()

        groupId = arguments?.getString("groupId")

        // Initialize UI elements
        val groupNameTextView = view.findViewById<TextView>(R.id.textViewGroupName)
        val groupDescriptionTextView = view.findViewById<TextView>(R.id.textViewGroupDescription)
        val editButton = view.findViewById<Button>(R.id.buttonEditGroup)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Fetch group data and display it
        groupId?.let { id ->
            firestore.collection("groups").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val groupName = document.getString("name")
                        val groupDescription = document.getString("description")
                        val createdBy = document.getString("createdBy")

                        groupNameTextView.text = groupName ?: "Unnamed Group"
                        groupDescriptionTextView.text = groupDescription ?: "No Description"

                        // Check if the current user is the creator
                        if (createdBy == currentUserId) {
                            editButton.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Group not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Set up Edit button click listener
        editButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
                putString("groupName", groupNameTextView.text.toString())
                putString("groupDescription", groupDescriptionTextView.text.toString())
            }
            findNavController().navigate(R.id.action_groupOverviewFragment_to_groupDetailFragment, bundle)
        }

        return view
    }
}

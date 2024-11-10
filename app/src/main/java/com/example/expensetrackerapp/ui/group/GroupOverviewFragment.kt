package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.expensetrackerapp.R
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

        val groupId = arguments?.getString("groupId")

        // Initialize UI elements for group name and description
        val groupNameTextView = view.findViewById<TextView>(R.id.textViewGroupName)
        val groupDescriptionTextView = view.findViewById<TextView>(R.id.textViewGroupDescription)

        // Fetch group data and display it
        groupId?.let { id ->
            firestore.collection("groups").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val groupName = document.getString("name")
                        val groupDescription = document.getString("description")

                        groupNameTextView.text = groupName ?: "Unnamed Group"
                        groupDescriptionTextView.text = groupDescription ?: "No Description"
                    } else {
                        Toast.makeText(requireContext(), "Group not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}

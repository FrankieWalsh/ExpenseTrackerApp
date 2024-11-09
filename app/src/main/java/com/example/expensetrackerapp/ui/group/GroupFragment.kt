package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.example.expensetrackerapp.model.Group
import android.app.AlertDialog
import android.widget.EditText
import android.util.Log
import android.widget.Button

class GroupFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var groupAdapter: GroupAdapter
    private val groups = mutableListOf<Group>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)

        firestore = FirebaseFirestore.getInstance()
        groupAdapter = GroupAdapter(
            groups,
            onClick = { group ->
                findNavController().navigate(R.id.action_groupFragment_to_groupDetailFragment)
            },
            onJoinClick = { group ->
                Toast.makeText(requireContext(), "Joining ${group.name}", Toast.LENGTH_SHORT).show()
                joinGroup(group)
            }
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGroups)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = groupAdapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fabCreateGroup)
        fab.setOnClickListener {
            createNewGroup()
        }

        fetchGroups()

        return view
    }

    private fun fetchGroups() {
        firestore.collection("groups")
            .get()
            .addOnSuccessListener { result ->
                groups.clear()
                for (document in result) {
                    val group = document.toObject(Group::class.java).apply { id = document.id }
                    groups.add(group)
                }
                groupAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching groups: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewGroup() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val groupNameEditText = dialogView.findViewById<EditText>(R.id.editTextGroupName)
        val groupDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextGroupDescription)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create New Group")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val createButton = dialogView.findViewById<Button>(R.id.buttonCreateGroup)
            createButton.setOnClickListener {
                Log.d("GroupFragment", "Create button clicked in dialog")

                val name = groupNameEditText.text.toString().trim()
                val description = groupDescriptionEditText.text.toString().trim()

                if (name.isEmpty()) {
                    Log.d("GroupFragment", "Group name is empty")
                    Toast.makeText(requireContext(), "Group name is required", Toast.LENGTH_SHORT).show()
                } else {
                    val group = Group(name = name, description = description)
                    Log.d("GroupFragment", "Attempting to add group to Firestore")
                    firestore.collection("groups")
                        .add(group)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Group created", Toast.LENGTH_SHORT).show()
                            fetchGroups()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Error creating group: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        dialog.show()
    }

    private fun joinGroup(group: Group) {
        Log.d("GroupFragment", "User joining group: ${group.name}")
    }
}

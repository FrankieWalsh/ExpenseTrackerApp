package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GroupFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)

        // Set up RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGroups)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = GroupAdapter(listOf("Group 1", "Group 2", "Group 3")) // Example data

        // Set up FAB to create a new group
        val fab = view.findViewById<FloatingActionButton>(R.id.fabCreateGroup)
        fab.setOnClickListener {
            Toast.makeText(requireContext(), "Create Group clicked", Toast.LENGTH_SHORT).show()
            // Implement navigation to CreateGroupFragment or open a dialog for group creation
        }
        return view
    }
}

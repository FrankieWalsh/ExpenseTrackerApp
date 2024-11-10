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
import com.example.expensetrackerapp.model.Group
import com.example.expensetrackerapp.model.Invitation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import android.widget.EditText
import android.widget.Button
import android.util.Log
import android.widget.TextView
import com.example.expensetrackerapp.model.GroupMember

class GroupFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var invitationAdapter: InvitationAdapter
    private val groups = mutableListOf<Group>()
    private val invitations = mutableListOf<Invitation>()
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)
        firestore = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Initialize Group Adapter
        groupAdapter = GroupAdapter(
            groups,
            onClick = { group -> findNavController().navigate(R.id.action_groupFragment_to_groupDetailFragment) },
            onJoinClick = { group -> joinGroup(group) }
        )

        // Initialize Invitation Adapter
        invitationAdapter = InvitationAdapter(
            invitations,
            onAccept = { invitation -> acceptInvitation(invitation) },
            onDecline = { invitation -> declineInvitation(invitation) }
        )

        // Set up RecyclerViews for groups and invitations
        view.findViewById<RecyclerView>(R.id.recyclerViewGroups).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }

        view.findViewById<RecyclerView>(R.id.recyclerViewInvitations).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = invitationAdapter
        }

        // Set up Floating Action Button to create new groups
        view.findViewById<FloatingActionButton>(R.id.fabCreateGroup).setOnClickListener {
            findNavController().navigate(R.id.action_groupFragment_to_groupDetailFragment)
        }

        // Fetch groups and invitations
        fetchGroupsAndInvitations()
        return view
    }

    private fun fetchGroupsAndInvitations() {
        // Fetch group memberships
        firestore.collection("group_members")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { memberResult ->
                val groupIds = memberResult.documents.mapNotNull { document ->
                    document.toObject(GroupMember::class.java)?.groupId
                }.toMutableSet()

                // Fetch pending invitations for the user
                firestore.collection("invitations")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { invitationResult ->
                        invitations.clear()
                        invitationResult.documents.forEach { document ->
                            val invitation = document.toObject(Invitation::class.java)
                            if (invitation != null) {
                                invitation.id = document.id
                                invitations.add(invitation)
                                groupIds.add(invitation.groupId) // Include invited groups in groupIds
                            }
                        }

                        // Update UI visibility for invitations
                        view?.findViewById<View>(R.id.invitationsContainer)?.visibility =
                            if (invitations.isEmpty()) View.GONE else View.VISIBLE
                        invitationAdapter.notifyDataSetChanged()

                        if (groupIds.isNotEmpty()) {
                            firestore.collection("groups")
                                .whereIn("id", groupIds.toList())
                                .get()
                                .addOnSuccessListener { groupsResult ->
                                    groups.clear()
                                    for (document in groupsResult) {
                                        val group = document.toObject(Group::class.java)
                                        group?.id = document.id
                                        groups.add(group)
                                    }
                                    groupAdapter.notifyDataSetChanged()
                                    toggleNoGroupsMessage()
                                }
                        } else {
                            groups.clear()
                            groupAdapter.notifyDataSetChanged()
                            toggleNoGroupsMessage()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleNoGroupsMessage() {
        val noGroupsTextView = view?.findViewById<TextView>(R.id.textViewNoGroups)
        if (groups.isEmpty() && invitations.isEmpty()) {
            noGroupsTextView?.visibility = View.VISIBLE
        } else {
            noGroupsTextView?.visibility = View.GONE
        }
    }


    private fun createNewGroup() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val groupNameEditText = dialogView.findViewById<EditText>(R.id.editTextGroupName)
        val groupDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextGroupDescription)

        AlertDialog.Builder(requireContext())
            .setTitle("Create New Group")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    dialogView.findViewById<Button>(R.id.buttonCreateGroup).setOnClickListener {
                        val name = groupNameEditText.text.toString().trim()
                        val description = groupDescriptionEditText.text.toString().trim()

                        if (name.isNotEmpty()) {
                            val group = Group(name = name, description = description, createdBy = userId)
                            firestore.collection("groups").add(group)
                                .addOnSuccessListener {
                                    fetchGroupsAndInvitations()
                                    dismiss()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(requireContext(), "Group name required", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                show()
            }
    }

    private fun joinGroup(group: Group) {
        Log.d("GroupFragment", "Joining group: ${group.name}")
    }

    private fun sendInvitation(groupId: String, invitedUserId: String) {
        val invitation = Invitation(groupId = groupId, userId = invitedUserId, invitedBy = userId, status = "pending")
        firestore.collection("invitations").add(invitation)
            .addOnSuccessListener { Toast.makeText(requireContext(), "Invitation sent", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun showInviteDialog(groupId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_invite_user, null)
        val userIdEditText = dialogView.findViewById<EditText>(R.id.editTextUserIdToInvite)

        AlertDialog.Builder(requireContext())
            .setTitle("Invite User to Group")
            .setView(dialogView)
            .setPositiveButton("Invite") { _, _ ->
                val invitedUserId = userIdEditText.text.toString().trim()
                if (invitedUserId.isNotEmpty()) sendInvitation(groupId, invitedUserId)
                else Toast.makeText(requireContext(), "Enter user ID", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun acceptInvitation(invitation: Invitation) {
        val groupMember = GroupMember(id = "", groupId = invitation.groupId, userId = userId)
        firestore.collection("group_members").add(groupMember)
            .addOnSuccessListener {
                firestore.collection("invitations").document(invitation.id)
                    .delete()
                    .addOnSuccessListener { fetchGroupsAndInvitations() }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error removing invitation: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding to group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineInvitation(invitation: Invitation) {
        firestore.collection("invitations").document(invitation.id)
            .delete()
            .addOnSuccessListener { fetchGroupsAndInvitations() }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error declining invitation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}

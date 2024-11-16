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
            onGroupClick = { group ->
                // Use the new action to navigate to GroupOverviewFragment, passing the group ID
                val bundle = Bundle().apply {
                    putString("groupId", group.id)
                }
                findNavController().navigate(R.id.action_groupFragment_to_groupTabsFragment, bundle)
            }
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
        // First, fetch only the groups the user is a member of from the `group_members` collection
        firestore.collection("group_members")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { memberResult ->
                val groupIds = memberResult.documents.mapNotNull { document ->
                    document.toObject(GroupMember::class.java)?.groupId
                }.toMutableSet()

                // Now, fetch groups based on the collected group IDs from `group_members`
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
                            toggleNoGroupsMessage() // Adjust visibility of "No groups" message based on membership
                        }
                } else {
                    // If no memberships, clear the group list and show "No groups" message
                    groups.clear()
                    groupAdapter.notifyDataSetChanged()
                    toggleNoGroupsMessage()
                }

                // Fetch pending invitations separately for display in the invitation list
                fetchPendingInvitations()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Separate function to fetch pending invitations
    private fun fetchPendingInvitations() {
        firestore.collection("invitations")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { invitationResult ->
                invitations.clear()

                if (invitationResult.documents.isEmpty()) {
                    // Log if no invitations were found
                    Log.d("GroupFragment", "No pending invitations found for user $userId")
                    view?.findViewById<View>(R.id.invitationsContainer)?.visibility = View.GONE
                    invitationAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Create a temporary map to store group names by groupId
                val groupIdToNameMap = mutableMapOf<String, String>()
                var invitationsProcessed = 0

                // Loop through each invitation document
                invitationResult.documents.forEach { document ->
                    val invitation = document.toObject(Invitation::class.java)
                    if (invitation != null) {
                        invitation.id = document.id

                        // Fetch group name if not cached
                        if (groupIdToNameMap.containsKey(invitation.groupId)) {
                            invitation.groupName = groupIdToNameMap[invitation.groupId]
                            invitations.add(invitation)
                            invitationsProcessed++
                        } else {
                            firestore.collection("groups").document(invitation.groupId)
                                .get()
                                .addOnSuccessListener { groupDoc ->
                                    val groupName = groupDoc.getString("name") ?: "Unnamed Group"
                                    groupIdToNameMap[invitation.groupId] = groupName
                                    invitation.groupName = groupName
                                    invitations.add(invitation)
                                    invitationsProcessed++

                                    // Update the adapter once all invitations have been processed
                                    if (invitationsProcessed == invitationResult.documents.size) {
                                        invitationAdapter.notifyDataSetChanged()
                                        view?.findViewById<View>(R.id.invitationsContainer)?.visibility = if (invitations.isEmpty()) View.GONE else View.VISIBLE
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("GroupFragment", "Error fetching group name: ${e.message}")
                                    invitationsProcessed++
                                    if (invitationsProcessed == invitationResult.documents.size) {
                                        invitationAdapter.notifyDataSetChanged()
                                        view?.findViewById<View>(R.id.invitationsContainer)?.visibility = if (invitations.isEmpty()) View.GONE else View.VISIBLE
                                    }
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GroupFragment", "Error fetching invitations: ${e.message}")
                Toast.makeText(requireContext(), "Error fetching invitations: ${e.message}", Toast.LENGTH_SHORT).show()
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
        // Add the user to `group_members` with the UID and delete the invitation
        val groupMember = GroupMember(id = "", groupId = invitation.groupId, userId = userId) // UID from Firebase Auth
        firestore.collection("group_members").add(groupMember)
            .addOnSuccessListener { documentRef ->
                // Set the document ID as the `id` field in Firestore for consistency
                documentRef.update("id", documentRef.id)
                    .addOnSuccessListener {
                        firestore.collection("invitations").document(invitation.id)
                            .delete()
                            .addOnSuccessListener { fetchGroupsAndInvitations() }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error removing invitation: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupFragment", "Error setting GroupMember ID: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding to group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineInvitation(invitation: Invitation) {
        // Simply delete the invitation document if the user declines
        firestore.collection("invitations").document(invitation.id)
            .delete()
            .addOnSuccessListener { fetchGroupsAndInvitations() }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error declining invitation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}

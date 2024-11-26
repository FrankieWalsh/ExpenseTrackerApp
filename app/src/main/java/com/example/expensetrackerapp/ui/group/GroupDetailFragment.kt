package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Group
import com.example.expensetrackerapp.model.Invitation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.EditText
import android.widget.Button
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.GroupMember

class GroupDetailFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private var isEditMode = false
    private var groupId: String? = null

    private val inviteEmails = mutableListOf<String>()
    private lateinit var inviteEmailAdapter: InviteEmailAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_detail, container, false)

        firestore = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        groupId = arguments?.getString("groupId")
        isEditMode = groupId != null

        val groupNameEditText = view.findViewById<EditText>(R.id.editTextGroupName)
        val groupDescriptionEditText = view.findViewById<EditText>(R.id.editTextGroupDescription)
        val inviteEmailEditText = view.findViewById<EditText>(R.id.editTextInviteEmail)
        val addEmailButton = view.findViewById<Button>(R.id.buttonAddEmail)
        val createGroupButton = view.findViewById<Button>(R.id.buttonCreateGroup)
        val deleteGroupButton = view.findViewById<Button>(R.id.buttonDeleteGroup)

        inviteEmailAdapter = InviteEmailAdapter(inviteEmails)
        view.findViewById<RecyclerView>(R.id.recyclerViewInvitedEmails).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inviteEmailAdapter
        }

        addEmailButton.setOnClickListener {
            val email = inviteEmailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                inviteEmails.add(email)
                inviteEmailEditText.text.clear()
                inviteEmailAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Added $email to invites", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Enter a valid email", Toast.LENGTH_SHORT).show()
            }
        }

        createGroupButton.text = if (isEditMode) "Update Group" else "Create Group"
        createGroupButton.setOnClickListener {
            val name = groupNameEditText.text.toString().trim()
            val description = groupDescriptionEditText.text.toString().trim()

            if (name.isNotEmpty()) {
                if (isEditMode) {
                    updateGroup(name, description)
                } else {
                    createGroup(name, description)
                }
            } else {
                Toast.makeText(requireContext(), "Group name required", Toast.LENGTH_SHORT).show()
            }
        }
        deleteGroupButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
        deleteGroupButton.setOnClickListener {
            groupId?.let { id ->
                deleteGroup(id)
            } ?: Toast.makeText(requireContext(), "Group ID not found", Toast.LENGTH_SHORT).show()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isEditMode) {
            // If in edit mode, pre-fill group details
            groupId?.let { id ->
                firestore.collection("groups").document(id).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            view.findViewById<EditText>(R.id.editTextGroupName).setText(document.getString("name"))
                            view.findViewById<EditText>(R.id.editTextGroupDescription).setText(document.getString("description"))
                        } else {
                            Toast.makeText(requireContext(), "Group not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun createGroup(name: String, description: String) {
        val group = Group(name = name, description = description, createdBy = userId)

        firestore.collection("groups").add(group)
            .addOnSuccessListener { groupDoc ->
                val groupId = groupDoc.id
                groupDoc.update("id", groupId)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Group created successfully", Toast.LENGTH_SHORT).show()

                        addGroupMember(groupId, userId, isCreator = true)

                        if (inviteEmails.isNotEmpty()) {
                            var invitationsProcessed = 0
                            for (email in inviteEmails) {
                                sendInvitation(groupId, email) {
                                    invitationsProcessed++
                                    if (invitationsProcessed == inviteEmails.size) {
                                        parentFragmentManager.popBackStack()
                                    }
                                }
                            }
                        } else {
                            parentFragmentManager.popBackStack()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error setting group ID: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error creating group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateGroup(name: String, description: String) {
        groupId?.let { id ->
            firestore.collection("groups").document(id)
                .update("name", name, "description", description)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Group updated successfully", Toast.LENGTH_SHORT).show()
                    if (inviteEmails.isNotEmpty()) {
                        var invitationsProcessed = 0
                        for (email in inviteEmails) {
                            sendInvitation(id, email) {
                                invitationsProcessed++
                                if (invitationsProcessed == inviteEmails.size) {
                                    parentFragmentManager.popBackStack()
                                }
                            }
                        }
                    } else {
                        parentFragmentManager.popBackStack() // Navigate back to the previous screen
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating group: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun addGroupMember(groupId: String, userId: String, isCreator: Boolean = false) {
        val groupMember = GroupMember(
            id = "",
            groupId = groupId,
            userId = userId
        )

        firestore.collection("group_members").add(groupMember)
            .addOnSuccessListener { documentRef ->
                val documentId = documentRef.id
                documentRef.update("id", documentId)
                    .addOnSuccessListener {
                        Log.d("GroupDetailFragment", "GroupMember added with ID: $documentId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupDetailFragment", "Error setting GroupMember ID: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding user to group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendInvitation(groupId: String, email: String, onComplete: () -> Unit) {
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (result.documents.isNotEmpty()) {
                    val invitedUserId = result.documents[0].id
                    val invitation = Invitation(
                        id = "",
                        groupId = groupId,
                        userId = invitedUserId,
                        invitedBy = this.userId,
                        status = "pending"
                    )

                    firestore.collection("invitations").add(invitation)
                        .addOnSuccessListener {
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Invitation sent to $email", Toast.LENGTH_SHORT).show()
                            }
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Error sending invitation: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            onComplete()
                        }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "User with email $email not found", Toast.LENGTH_SHORT).show()
                    }
                    onComplete()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error finding user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }
    }

    private fun deleteGroup(groupId: String) {
        firestore.collection("groups").document(groupId).delete()
            .addOnSuccessListener {
                // Borrar los miembros del grupo
                firestore.collection("group_members")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .addOnSuccessListener { members ->
                        for (member in members) {
                            firestore.collection("group_members").document(member.id).delete()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupDetailFragment", "Error deleting group members: ${e.message}")
                    }

                // Borrar las invitaciones relacionadas
                firestore.collection("invitations")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .addOnSuccessListener { invitations ->
                        for (invitation in invitations) {
                            firestore.collection("invitations").document(invitation.id).delete()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupDetailFragment", "Error deleting invitations: ${e.message}")
                    }

                Toast.makeText(requireContext(), "Group deleted successfully", Toast.LENGTH_SHORT).show()
                val navController = parentFragmentManager.findFragmentById(R.id.nav_host_fragment)?.findNavController()
                navController?.navigate(R.id.action_groupDetailFragment_to_groupFragment)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error deleting group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}

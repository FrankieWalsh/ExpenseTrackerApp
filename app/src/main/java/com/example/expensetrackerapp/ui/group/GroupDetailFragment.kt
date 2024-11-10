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
import com.example.expensetrackerapp.model.GroupMember

class GroupDetailFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private var isEditMode = false
    private var groupId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Check if groupId is provided as an argument for Edit Mode
        groupId = arguments?.getString("groupId")
        isEditMode = groupId != null

        // Initialize UI elements
        val groupNameEditText = view.findViewById<EditText>(R.id.editTextGroupName)
        val groupDescriptionEditText = view.findViewById<EditText>(R.id.editTextGroupDescription)
        val inviteEmailEditText = view.findViewById<EditText>(R.id.editTextInviteEmail)
        val addEmailButton = view.findViewById<Button>(R.id.buttonAddEmail)
        val createGroupButton = view.findViewById<Button>(R.id.buttonCreateGroup)

        // Placeholder list for email invites
        val inviteEmails = mutableListOf<String>()

        // Add email to the invite list
        addEmailButton.setOnClickListener {
            val email = inviteEmailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                inviteEmails.add(email)
                inviteEmailEditText.text.clear()
                Toast.makeText(requireContext(), "Added $email to invites", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Enter a valid email", Toast.LENGTH_SHORT).show()
            }
        }

        // Create or Edit group based on mode
        createGroupButton.text = if (isEditMode) "Update Group" else "Create Group"
        createGroupButton.setOnClickListener {
            val name = groupNameEditText.text.toString().trim()
            val description = groupDescriptionEditText.text.toString().trim()

            if (name.isNotEmpty()) {
                if (isEditMode) {
                    // Update the group (future implementation)
                    Toast.makeText(requireContext(), "Edit group functionality not yet implemented", Toast.LENGTH_SHORT).show()
                } else {
                    // Create new group
                    createGroup(name, description, inviteEmails)
                }
            } else {
                Toast.makeText(requireContext(), "Group name required", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun createGroup(name: String, description: String, inviteEmails: List<String>) {
        val group = Group(name = name, description = description, createdBy = userId)

        firestore.collection("groups").add(group)
            .addOnSuccessListener { groupDoc ->
                val groupId = groupDoc.id
                groupDoc.update("id", groupId)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Group created successfully", Toast.LENGTH_SHORT).show()

                        // Send invitations to each email
                        for (email in inviteEmails) {
                            sendInvitation(groupId, email)
                        }

                        parentFragmentManager.popBackStack() // Navigate back to GroupFragment
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error setting group ID: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error creating group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to add a user as a member of a group
    private fun addGroupMember(groupId: String, userId: String, isCreator: Boolean = false) {
        // Create a GroupMember instance with an empty `id` field
        val groupMember = GroupMember(
            id = "", // This will be set by Firestore once we have the document ID
            groupId = groupId,
            userId = userId
        )

        // Add the group member to Firestore
        firestore.collection("group_members").add(groupMember)
            .addOnSuccessListener { documentRef ->
                val documentId = documentRef.id // Firestore-generated document ID

                // Update the `id` field in Firestore to match the document ID
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

    private fun sendInvitation(groupId: String, email: String) {
        // Look up the user by email in the `users` collection
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (result.documents.isNotEmpty()) {
                    val userId = result.documents[0].id
                    val invitation = Invitation(
                        id = "", // Firestore will auto-generate the ID
                        groupId = groupId,
                        userId = userId,
                        invitedBy = this.userId,
                        status = "pending"
                    )

                    // Save the invitation to the `invitations` collection
                    firestore.collection("invitations").add(invitation)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Invitation sent to $email", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error sending invitation: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // User with the provided email was not found
                    Toast.makeText(requireContext(), "User with email $email not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error finding user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}

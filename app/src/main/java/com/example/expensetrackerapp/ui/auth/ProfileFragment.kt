package com.example.expensetrackerapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.expensetrackerapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""

        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val editTextEmail = view.findViewById<EditText>(R.id.editTextEmail)
        val editTextPhoneNumber = view.findViewById<EditText>(R.id.editTextPhoneNumber)
        val buttonSave = view.findViewById<Button>(R.id.buttonSave)
        val buttonChangePassword = view.findViewById<Button>(R.id.buttonChangePassword)
        val buttonLogout: Button = view.findViewById(R.id.buttonLogout)

        // Load user data
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editTextName.setText(document.getString("name") ?: "")
                    editTextEmail.setText(document.getString("email") ?: "")
                    editTextPhoneNumber.setText(document.getString("phoneNumber") ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Save changes
        buttonSave.setOnClickListener {
            val name = editTextName.text.toString()
            val email = editTextEmail.text.toString()
            val phoneNumber = editTextPhoneNumber.text.toString()

            if (email.isEmpty() || name.isEmpty()) {
                Toast.makeText(requireContext(), "Name and email are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("users").document(userId).update(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "phoneNumber" to phoneNumber
                )
            ).addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Change password
        buttonChangePassword.setOnClickListener {
            auth.sendPasswordResetEmail(auth.currentUser?.email ?: "").addOnSuccessListener {
                Toast.makeText(requireContext(), "Password reset email sent", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error sending password reset email: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogout.setOnClickListener {
            auth.signOut()

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()

            findNavController().navigate(R.id.action_profileFragment_to_loginFragment, null, navOptions)

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}

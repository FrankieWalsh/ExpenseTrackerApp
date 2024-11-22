package com.example.expensetrackerapp.ui.auth

import com.example.expensetrackerapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.expensetrackerapp.R
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import android.graphics.Paint

class RegisterFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailEditText = view.findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = view.findViewById<EditText>(R.id.editTextPassword)
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val registerButton = view.findViewById<Button>(R.id.buttonRegister)
        val loginLink = view.findViewById<TextView>(R.id.loginLink)
        loginLink.paintFlags = loginLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter name, email, and password", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                            val firestoreUser = User(
                                id = uid,
                                name = name,
                                email = email,
                                phoneNumber = "",  // You can add a field for phone number if needed
                                notificationPreferences = emptyMap()
                            )

                            // Save the user data in Firestore using UID as the document ID
                            firestore.collection("users").document(uid).set(firestoreUser)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                                    // Navigate to the login screen or main app screen
                                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        return view
    }
}

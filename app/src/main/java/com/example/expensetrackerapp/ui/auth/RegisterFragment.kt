package com.example.expensetrackerapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.expensetrackerapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        val nameEditText = view.findViewById<EditText>(R.id.editTextName) // New name field
        val registerButton = view.findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val userId = user.uid
                                val userDocument = mapOf(
                                    "name" to name,
                                    "email" to email,
                                    "userId" to userId
                                )
                                firestore.collection("users").document(userId).set(userDocument)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(requireContext(), "Error saving user info", Toast.LENGTH_SHORT).show()
                                    }
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

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

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get references to UI components
        val emailEditText = view.findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = view.findViewById<EditText>(R.id.editTextPassword)
        val registerButton = view.findViewById<Button>(R.id.buttonRegister)

        // Set up the Register button click listener
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                // Create user with email and password
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Registration successful, navigate to login
                            Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        } else {
                            // Registration failed
                            Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        return view
    }
}

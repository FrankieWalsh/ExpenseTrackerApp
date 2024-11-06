package com.example.expensetrackerapp
import com.google.firebase.FirebaseApp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Ensure Firebase is initialized
        setContentView(R.layout.activity_main) // This loads the NavHostFragment from activity_main.xml
    }
}

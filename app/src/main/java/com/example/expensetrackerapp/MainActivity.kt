package com.example.expensetrackerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.expensetrackerapp.ui.auth.LoginFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // This loads the NavHostFragment from activity_main.xml
    }
}

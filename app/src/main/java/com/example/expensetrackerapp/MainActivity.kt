package com.example.expensetrackerapp
import com.google.firebase.FirebaseApp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.groupFragment, R.id.profileFragment) // Fragments where back button is not needed
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Dynamically show/hide toolbar based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.registerFragment) {
                // Hide the toolbar
                toolbar.visibility = View.GONE
            } else {
                // Show the toolbar
                toolbar.visibility = View.VISIBLE
            }

            // Set toolbar title dynamically
            toolbar.title = when (destination.id) {
                R.id.groupFragment -> "Home"
                R.id.groupTabsFragment -> "Group Overview"
                R.id.profileFragment -> "Profile"
                else -> destination.label.toString()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profileFragment -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.profileFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

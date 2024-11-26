package com.example.expensetrackerapp
import com.google.firebase.FirebaseApp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.groupFragment) // Fragments where back button is not needed
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
            R.id.darkmodeFragment -> {
                // Alternar entre Dark Mode y Light Mode
                toggleDarkMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun toggleDarkMode() {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isDarkMode) {
            // Light Mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            // Dark Mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        saveThemePreference(!isDarkMode)
    }
    private fun saveThemePreference(isDarkMode: Boolean) {
        val sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isDarkMode", isDarkMode).apply()
    }


}

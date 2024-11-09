package com.example.expensetrackerapp.model

data class User(
    var id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val notificationPreferences: Map<String, Boolean> = emptyMap()
)
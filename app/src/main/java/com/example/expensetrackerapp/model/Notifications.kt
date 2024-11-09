package com.example.expensetrackerapp.model

data class Notification(
    var id: String = "",
    val userId: String = "",
    val groupId: String? = null,
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

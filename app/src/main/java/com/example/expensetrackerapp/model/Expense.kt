package com.example.expensetrackerapp.model

data class Expense(
    var id: String = "",
    val groupId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val payerId: String = "",
    val receiptPhotoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val category: String = "General"
)

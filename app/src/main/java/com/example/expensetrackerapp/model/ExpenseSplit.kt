// ExpenseSplit.kt
package com.example.expensetrackerapp.model

data class ExpenseSplit(
    var id: String = "",
    val expenseId: String = "",
    val userId: String = "",
    val groupId: String = "", // New groupId field
    val owedTo: String = "",  // New owedTo field for the expense creator
    val amount: Double = 0.0,
    var hasPaid: Boolean = false
)

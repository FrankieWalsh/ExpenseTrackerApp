package com.example.expensetrackerapp.model

data class ExpenseSplit(
    var id: String = "",
    val expenseId: String = "",
    val userId: String = "",
    val amount: Double = 0.0
)

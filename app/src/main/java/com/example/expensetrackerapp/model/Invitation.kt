package com.example.expensetrackerapp.model

data class Invitation(
    var id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val invitedBy: String = "",
    var status: String = "pending",
    var groupName: String? = null
)
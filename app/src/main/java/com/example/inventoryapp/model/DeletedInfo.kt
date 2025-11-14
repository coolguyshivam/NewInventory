package com.example.inventoryapp.model

data class DeletedInfo(
    val deletedBy: String = "",
    val deletedAt: String = "",
    val deletionReason: String = ""
)
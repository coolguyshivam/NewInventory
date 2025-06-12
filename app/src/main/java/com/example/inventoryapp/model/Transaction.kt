package com.example.inventoryapp.model

data class Transaction(
    val id: String = "",
    val type: String = "",
    val model: String = "",
    val serial: String = "",
    val phone: String = "",
    val aadhaar: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = "",
    val quantity: Int = 1,
    val timestamp: Long = 0L
)
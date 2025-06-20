package com.example.inventoryapp.model

import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

data class Transaction(
    val type: String = "",
    val model: String = "",
    val serial: String = "",
    val phone: String = "",
    val aadhaar: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = "",
    val quantity: Int = 1,
    val timestamp: Long = 0L,
    val imageUrls: List<String> = emptyList()
)
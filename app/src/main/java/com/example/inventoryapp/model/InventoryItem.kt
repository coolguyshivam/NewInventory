package com.example.inventoryapp.model

data class InventoryItem(
    val id: String = "",
    val name: String = "",         // Human-readable name
    val quantity: Int = 0,         // Inventory stock count
    val model: String = "",
    val serial: String = "",
    val phone: String = "",
    val aadhaar: String = "",
    val description: String = "",
    val date: String = "",
    val timestamp: Long = 0L
)
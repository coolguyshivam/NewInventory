package com.example.inventoryapp.model

data class InventoryItem(
    val serial: String = "",
    val name: String = "",         // Human-readable name
	val model: String = "",
    var quantity: Int = 0,         // Inventory stock count
    val phone: String = "",
    val aadhaar: String = "",
    val description: String = "",
    val date: String = "",
    val timestamp: Long = 0L
)
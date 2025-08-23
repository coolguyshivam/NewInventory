package com.example.inventoryapp.model

/**
 * Enum representing the status of an inventory item
 */
enum class InventoryStatus {
    AVAILABLE,  // Item is available for sale
    REPAIR,     // Item is in repair mode - cannot be sold
    SOLD        // Item has been sold
}

/**
 * Extension functions for InventoryStatus
 */
fun InventoryStatus.canBeSold(): Boolean = this == InventoryStatus.AVAILABLE

fun InventoryStatus.canBeRepaired(): Boolean = this == InventoryStatus.AVAILABLE

fun InventoryStatus.canBeReturned(): Boolean = this == InventoryStatus.SOLD || this == InventoryStatus.REPAIR

fun InventoryStatus.getDisplayName(): String = when (this) {
    InventoryStatus.AVAILABLE -> "Available"
    InventoryStatus.REPAIR -> "Repair Mode"
    InventoryStatus.SOLD -> "Sold"
}

fun InventoryStatus.getDisplayColor(): androidx.compose.ui.graphics.Color = when (this) {
    InventoryStatus.AVAILABLE -> androidx.compose.ui.graphics.Color.Green
    InventoryStatus.REPAIR -> androidx.compose.ui.graphics.Color.Red
    InventoryStatus.SOLD -> androidx.compose.ui.graphics.Color.Gray
}
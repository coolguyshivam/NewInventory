package com.example.inventoryapp.model

/**
 * Represents a single inventory item in the Firestore database.
 * Now supports multiple images per item and status-based workflow.
 */
data class InventoryItem(
    val serial: String = "",                    // Unique serial number (document ID)
    val name: String = "",                      // Item name or description
    val model: String = "",                     // Model (required, used for analytics/filter)
    val quantity: Int = 0,                      // Current quantity in stock
    val phone: String = "",                     // Optional: associated phone number (customer mobile)
    val aadhaar: String = "",                   // Optional: associated Aadhaar number (customer adhar)
    val description: String = "",               // Optional: notes or description
    val date: String = "",                      // Creation/purchase date ("yyyy-MM-dd")
    val timestamp: Long = 0L,                   // Unix time in millis for sorting/filtering
    val imageUrls: List<String> = emptyList(),   // Supports multiple images per item
    val purchasePrice: Double = 0.0,            // Purchase price of the item
    val customerName: String = "",              // Customer name
    
    // New status-based approach
    val status: ItemStatus = ItemStatus.AVAILABLE, // Primary status field
    
    // Legacy fields for backward compatibility - compute from status
    val isSold: Boolean = status == ItemStatus.SOLD,
    val isInRepair: Boolean = status == ItemStatus.REPAIR
) {
    // Helper methods for business rules
    fun canMarkRepair(): Boolean = status == ItemStatus.AVAILABLE
    fun canReturn(): Boolean = status == ItemStatus.REPAIR || status == ItemStatus.SOLD  
    fun canSell(): Boolean = status == ItemStatus.AVAILABLE && quantity > 0
    fun canDelete(): Boolean = status == ItemStatus.AVAILABLE || status == ItemStatus.REPAIR
    
    // Get computed status from legacy fields (for existing data)
    fun getComputedStatus(): ItemStatus = when {
        isSold -> ItemStatus.SOLD
        isInRepair -> ItemStatus.REPAIR
        else -> ItemStatus.AVAILABLE
    }
}
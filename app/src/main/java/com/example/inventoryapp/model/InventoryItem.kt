package com.example.inventoryapp.model

/**
 * Represents a single inventory item in the Firestore database.
 * Now supports multiple images per item and status management.
 */
data class InventoryItem(
    val serial: String = "",                    // Unique serial number (document ID)
    val name: String = "",                      // Item name or description
    val model: String = "",                     // Model (required, used for analytics/filter)
    val quantity: Int = 0,                      // Current quantity in stock
    val phone: String = "",                     // Optional: associated phone number
    val aadhaar: String = "",                   // Optional: associated Aadhaar number
    val description: String = "",               // Optional: notes or description
    val date: String = "",                      // Creation/purchase date ("yyyy-MM-dd")
    val timestamp: Long = 0L,                   // Unix time in millis for sorting/filtering
    val imageUrls: List<String> = emptyList(),   // Supports multiple images per item
    val status: InventoryStatus = InventoryStatus.AVAILABLE, // New status field
    // Legacy fields for backward compatibility - will be deprecated
    val isSold: Boolean = false,
    val isInRepair: Boolean = false
) {
    /**
     * Gets the current status, migrating from legacy fields if needed
     */
    fun getCurrentStatus(): InventoryStatus {
        return when {
            // If status is explicitly set and not default, use it
            status != InventoryStatus.AVAILABLE -> status
            // Otherwise, migrate from legacy fields
            isSold -> InventoryStatus.SOLD
            isInRepair -> InventoryStatus.REPAIR
            else -> InventoryStatus.AVAILABLE
        }
    }
    
    /**
     * Checks if item can be sold based on current status
     */
    fun canBeSold(): Boolean = getCurrentStatus().canBeSold()
    
    /**
     * Checks if item can be moved to repair
     */
    fun canBeRepaired(): Boolean = getCurrentStatus().canBeRepaired()
    
    /**
     * Checks if item can be returned
     */
    fun canBeReturned(): Boolean = getCurrentStatus().canBeReturned()
    
    /**
     * Creates a new item with updated status
     */
    fun withStatus(newStatus: InventoryStatus): InventoryItem {
        return this.copy(
            status = newStatus,
            // Update legacy fields for backward compatibility
            isSold = newStatus == InventoryStatus.SOLD,
            isInRepair = newStatus == InventoryStatus.REPAIR
        )
    }
}
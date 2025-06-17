package com.example.inventoryapp.data

data class InventoryItem(
    val name: String,
    val serial: String,
    val status: String = "Available"
)

class InventoryRepository {
    fun getInventory(): List<InventoryItem> {
        return listOf(
            InventoryItem("Redmi Note 10", "123ABC"),
            InventoryItem("Samsung A51", "456DEF"),
            InventoryItem("Vivo Y20", "789GHI")
        )
    }
}

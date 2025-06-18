package com.example.inventoryapp.data

import com.example.inventoryapp.model.Transaction

data class InventoryItem(
    val name: String,
    val serial: String,
    val status: String = "Available"
)

class InventoryRepository {

    // In-memory storage for demo purposes
    private val inventory = mutableListOf(
        InventoryItem("Redmi Note 10", "123ABC"),
        InventoryItem("Samsung A51", "456DEF"),
        InventoryItem("Vivo Y20", "789GHI")
    )

    private val transactions = mutableListOf<Transaction>()

    // Returns all inventory items
    fun getInventory(): Result<List<InventoryItem>> {
        return try {
            Result.Success(inventory)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Returns all transactions
    fun getAllTransactions(): Result<List<Transaction>> {
        return try {
            Result.Success(transactions)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Adds a transaction
    fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            transactions.add(transaction)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Finds an inventory item by serial number
    fun getItemBySerial(serial: String): InventoryItem? {
        return inventory.find { it.serial == serial }
    }
}
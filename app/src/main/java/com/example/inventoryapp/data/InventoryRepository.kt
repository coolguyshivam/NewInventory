package com.example.inventoryapp.data

import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction

class InventoryRepository {

    private val inventory = mutableListOf(
        InventoryItem(serial = "123ABC", name = "Redmi Note 10"),
        InventoryItem(serial = "456DEF", name = "Samsung A51"),
        InventoryItem(serial = "789GHI", name = "Vivo Y20")
    )

    private val transactions = mutableListOf<Transaction>()

    fun getInventory(): Result<List<InventoryItem>> {
        return try {
            Result.Success(inventory)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun getAllTransactions(): Result<List<Transaction>> {
        return try {
            Result.Success(transactions)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            transactions.add(transaction)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun getItemBySerial(serial: String): InventoryItem? {
        return inventory.find { it.serial == serial }
    }
}
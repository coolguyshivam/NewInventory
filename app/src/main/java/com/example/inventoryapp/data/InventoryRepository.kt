package com.example.inventoryapp.data

import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InventoryRepository {
    private val inventory = mutableListOf(
        InventoryItem(serial = "123ABC", name = "Redmi Note 10", quantity = 10),
        InventoryItem(serial = "456DEF", name = "Samsung A51", quantity = 8),
        InventoryItem(serial = "789GHI", name = "Vivo Y20", quantity = 5)
    )
    private val transactions = mutableListOf<Transaction>()

    // Flow for inventory
    private val inventoryFlow = MutableStateFlow(inventory.toList())
    fun getInventoryFlow(): Flow<List<InventoryItem>> = inventoryFlow.asStateFlow()

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
            val item = inventory.find { it.serial == transaction.serial }
            if (item != null) {
                when (transaction.type) {
                    "Purchase" -> item.quantity += transaction.quantity
                    "Sale" -> item.quantity -= transaction.quantity
                }
            } else if (transaction.type == "Purchase") {
                inventory.add(
                    InventoryItem(
                        serial = transaction.serial,
                        name = transaction.model,
                        quantity = transaction.quantity
                    )
                )
            }
            inventoryFlow.value = inventory.toList() // update flow
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun getItemBySerial(serial: String): InventoryItem? {
        return inventory.find { it.serial == serial }
    }
}
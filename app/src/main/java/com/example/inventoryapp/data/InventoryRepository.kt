package com.example.inventoryapp.data

import com.example.inventoryapp.model.Transaction
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.data.Result

// Local in-memory data stores for now
private val inventoryItems = mutableListOf(
    InventoryItem(serial = "S1001", model = "ModelX", quantity = 10),
    InventoryItem(serial = "S1002", model = "ModelY", quantity = 5)
)
private val transactions = mutableListOf<Transaction>()

class InventoryRepository {

    // --------- LOCAL IMPLEMENTATION ---------
    suspend fun getAllModels(): List<String> {
        return inventoryItems.map { it.model }.distinct()
    }

    suspend fun getItemBySerial(serial: String): InventoryItem? {
        return inventoryItems.find { it.serial == serial }
    }

    suspend fun getAllTransactions(): Result<List<Transaction>> {
        return Result.Success(transactions.toList())
    }

    suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        // Update inventory locally
        when (transaction.type) {
            "Purchase" -> {
                if (inventoryItems.any { it.serial == transaction.serial }) {
                    return Result.Error(Exception("Serial already exists"))
                }
                inventoryItems.add(
                    InventoryItem(
                        serial = transaction.serial,
                        model = transaction.model,
                        quantity = transaction.quantity
                    )
                )
            }
            "Sale" -> {
                val item = inventoryItems.find { it.serial == transaction.serial }
                if (item == null || item.quantity < transaction.quantity) {
                    return Result.Error(Exception("Not in inventory or not enough stock"))
                }
                item.quantity -= transaction.quantity
            }
            // Add logic for other types if needed
        }
        transactions.add(transaction)
        return Result.Success(Unit)
    }

    // --------- FIREBASE IMPLEMENTATION (example, replace above when ready) ---------
    /*
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun getAllModels(): List<String> {
        return try {
            val snapshot = db.collection("inventory").get().await()
            snapshot.documents.mapNotNull { it.getString("model") }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getItemBySerial(serial: String): InventoryItem? {
        return try {
            val snapshot = db.collection("inventory")
                .whereEqualTo("serial", serial)
                .get().await()
            val doc = snapshot.documents.firstOrNull()
            doc?.toObject(InventoryItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllTransactions(): Result<List<Transaction>> {
        return try {
            val snapshot = db.collection("transactions").get().await()
            val txs = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
            Result.Success(txs)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            db.collection("transactions").add(transaction).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    */
}
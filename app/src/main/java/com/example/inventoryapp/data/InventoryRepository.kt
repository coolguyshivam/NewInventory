package com.example.inventoryapp.data

import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InventoryRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // Fetch all inventory items (could be filtered or aggregated as needed)
    suspend fun getInventory(): Result<List<InventoryItem>> = try {
        val snapshot = db.collection("inventory").get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(InventoryItem::class.java)?.copy(id = doc.id)
        }
        Result.Success(items)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Add a new inventory item
    suspend fun addInventoryItem(item: InventoryItem): Result<Unit> = try {
        db.collection("inventory").add(item).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Update an existing inventory item by document ID
    suspend fun updateInventoryItem(item: InventoryItem): Result<Unit> = try {
        if (item.id.isNotEmpty()) {
            db.collection("inventory").document(item.id).set(item).await()
            Result.Success(Unit)
        } else {
            Result.Error(Exception("Invalid item ID"))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Fetch all transactions (for reporting/history)
    suspend fun getAllTransactions(): Result<List<Transaction>> = try {
        val snapshot = db.collection("transactions").get().await()
        val txs = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Transaction::class.java)?.copy(id = doc.id)
        }
        Result.Success(txs)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Add a new transaction (purchase or sale)
    suspend fun addTransaction(transaction: Transaction): Result<Unit> = try {
        db.collection("transactions").add(transaction).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
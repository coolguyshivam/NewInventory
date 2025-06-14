package com.example.inventoryapp.data

import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InventoryRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun getInventory(): Result<List<InventoryItem>> = try {
        val snapshot = db.collection("transactions")
            .whereEqualTo("type", "Purchase")
            .get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(InventoryItem::class.java)?.copy(id = doc.id)
        }
        Result.Success(items)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun addInventoryItem(item: InventoryItem): Result<Unit> = try {
        db.collection("transactions").add(item).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun addTransaction(transaction: Transaction): Result<Unit> = try {
        db.collection("transactions").add(transaction).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
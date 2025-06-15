package com.example.inventoryapp.data

import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InventoryRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getItemBySerial(serial: String): InventoryItem? {
        return try {
            val snapshot = firestore.collection("inventory")
                .whereEqualTo("serial", serial)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                snapshot.documents.first().toObject(InventoryItem::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllModelNames(): List<String> {
        return try {
            val snapshot = firestore.collection("inventory").get().await()
            snapshot.documents.mapNotNull {
                it.getString("model")
            }.toSet().sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addTransaction(transaction: Transaction): Result<Void?> {
        return try {
            val ref = firestore.collection("transactions").document()
            ref.set(transaction).await()
            Result.Success(null)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Add more methods if needed...
}


package com.example.inventoryapp.data

import android.net.Uri
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}

class InventoryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val inventoryRef = db.collection("inventory")
    private val transactionsRef = db.collection("transactions")
    private val storageRef = storage.reference

    /** Returns the first InventoryItem whose 'serial' field matches the given serial, or null if not found. */
    suspend fun getItemBySerial(serial: String): InventoryItem? {
        return try {
            val query = inventoryRef.whereEqualTo("serial", serial)
                .limit(1)
                .get()
                .await()
            query.documents.firstOrNull()?.toObject(InventoryItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getInventory(): Result<List<InventoryItem>> {
        return try {
            val snapshot = inventoryRef.get().await()
            val items = snapshot.toObjects(InventoryItem::class.java)
            Result.Success(items)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAllTransactions(): Result<List<Transaction>> {
        return try {
            val snapshot = transactionsRef.get().await()
            val transactions = snapshot.toObjects(Transaction::class.java)
            Result.Success(transactions)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            transactionsRef.add(transaction).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateInventory(item: InventoryItem): Result<Unit> {
        return try {
            // Uses document ID as item's serial
            inventoryRef.document(item.serial).set(item).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteInventoryItem(serial: String): Result<Unit> {
        return try {
            inventoryRef.document(serial).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun uploadImage(uri: Uri, fileName: String): Result<String> {
        return try {
            val ref = storageRef.child("images/$fileName")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.Success(url)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // This method is kept for compatibility, but is not used for model autofill in TransactionScreen.
    suspend fun getInventoryItemBySerial(serial: String): Result<InventoryItem?> {
        return try {
            val snapshot = inventoryRef.document(serial).get().await()
            val item = snapshot.toObject(InventoryItem::class.java)
            Result.Success(item)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
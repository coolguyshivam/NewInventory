package com.example.inventoryapp.data

import android.net.Uri
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class InventoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val inventoryRef = db.collection("inventory")
    private val transactionsRef = db.collection("transactions")
    private val storageRef = FirebaseStorage.getInstance().reference

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

    suspend fun addTransaction(transaction: Transaction): Result<Void?> {
        return try {
            transactionsRef.add(transaction).await()
            Result.Success(null)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateInventory(item: InventoryItem): Result<Void?> {
        return try {
            inventoryRef.document(item.serial).set(item).await()
            Result.Success(null)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteInventoryItem(serial: String): Result<Void?> {
        return try {
            inventoryRef.document(serial).delete().await()
            Result.Success(null)
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

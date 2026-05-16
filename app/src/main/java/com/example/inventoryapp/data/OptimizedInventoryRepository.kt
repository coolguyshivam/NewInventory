package com.example.inventoryapp.data

import com.example.inventoryapp.model.InventoryItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Optimized repository with pagination and caching for better performance
 */
class OptimizedInventoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val itemsCollection = db.collection("inventory")
    private var lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private val PAGE_SIZE = 20L
    
    // Local cache
    private val cache = mutableMapOf<String, InventoryItem>()
    private var cacheTimestamp = 0L
    private val CACHE_DURATION = 5 * 60 * 1000 // 5 minutes
    
    /**
     * Get items with pagination for better performance
     */
    suspend fun getInventoryPaginated(
        pageNumber: Int = 0,
        model: String? = null,
        status: String? = null
    ): Result<Pair<List<InventoryItem>, Boolean>> = try {
        var query: Query = itemsCollection
        
        // Apply filters
        if (!model.isNullOrEmpty()) {
            query = query.whereEqualTo("model", model)
        }
        if (!status.isNullOrEmpty()) {
            query = query.whereEqualTo("status", status)
        }
        
        // Order by timestamp for consistency
        query = query.orderBy("timestamp", Query.Direction.DESCENDING)
        
        // Pagination
        if (pageNumber > 0 && lastDocument != null) {
            query = query.startAfter(lastDocument!!)
        }
        
        query = query.limit(PAGE_SIZE + 1) // Get one extra to check if more exists
        
        val snapshot = query.get().await()
        val items = mutableListOf<InventoryItem>()
        var hasMore = false
        
        snapshot.documents.forEachIndexed { index, doc ->
            if (index < PAGE_SIZE) {
                items.add(doc.toObject(InventoryItem::class.java) ?: return@forEachIndexed)
            } else {
                hasMore = true
            }
        }
        
        // Update last document for next page
        if (items.isNotEmpty()) {
            lastDocument = snapshot.documents[items.size - 1]
        }
        
        // Update cache
        items.forEach { cache[it.serial] = it }
        cacheTimestamp = System.currentTimeMillis()
        
        Result.Success(Pair(items, hasMore))
    } catch (e: Exception) {
        Result.Error(e)
    }
    
    /**
     * Fast filter with local cache fallback
     */
    suspend fun filterItems(
        searchQuery: String,
        model: String? = null,
        status: String? = null
    ): Result<List<InventoryItem>> = try {
        var query: Query = itemsCollection
        
        // Apply filters
        if (!model.isNullOrEmpty()) {
            query = query.whereEqualTo("model", model)
        }
        if (!status.isNullOrEmpty()) {
            query = query.whereEqualTo("status", status)
        }
        
        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { 
            it.toObject(InventoryItem::class.java) 
        }
        
        // Filter by search query (serial, name, customer name)
        val filtered = items.filter { item ->
            item.serial.contains(searchQuery, ignoreCase = true) ||
            item.name.contains(searchQuery, ignoreCase = true) ||
            item.customerName.contains(searchQuery, ignoreCase = true)
        }
        
        Result.Success(filtered)
    } catch (e: Exception) {
        Result.Error(e)
    }
    
    /**
     * Get item from cache or Firestore
     */
    suspend fun getItemOptimized(serial: String): Result<InventoryItem?> = try {
        // Check cache first
        val now = System.currentTimeMillis()
        if (now - cacheTimestamp < CACHE_DURATION && cache.containsKey(serial)) {
            return Result.Success(cache[serial])
        }
        
        // Fetch from Firestore
        val doc = itemsCollection.document(serial).get().await()
        val item = doc.toObject(InventoryItem::class.java)
        item?.let { cache[serial] = it }
        
        Result.Success(item)
    } catch (e: Exception) {
        Result.Error(e)
    }
    
    /**
     * Reset pagination
     */
    fun resetPagination() {
        lastDocument = null
    }
    
    /**
     * Clear cache manually
     */
    fun clearCache() {
        cache.clear()
        cacheTimestamp = 0L
    }
}

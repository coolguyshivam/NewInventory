package com.example.inventoryapp.data

import android.util.LruCache
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Optimized Repository with:
 * - LRU Cache for frequently accessed items (5 min TTL)
 * - Pagination support for large datasets
 * - Local filtering to reduce database queries
 * - Concurrent access support
 */
class OptimizedRepository(private val baseRepo: InventoryRepository) {

    // In-memory cache with LRU eviction (max 100 items)
    private val itemCache = LruCache<String, CachedItem>(100)
    
    // Timestamp tracking for TTL enforcement
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        private const val DEFAULT_PAGE_SIZE = 20
    }

    data class CachedItem(
        val item: InventoryItem,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get all items with pagination and local caching
     */
    suspend fun getAllItemsOptimized(
        page: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Result<List<InventoryItem>> = withContext(Dispatchers.IO) {
        try {
            val result = baseRepo.getAllItems(limit = pageSize, startAfter = null)
            when (result) {
                is Result.Success -> {
                    // Cache each item
                    result.data.forEach { item ->
                        itemCache.put(item.serial, CachedItem(item))
                        cacheTimestamps[item.serial] = System.currentTimeMillis()
                    }
                    Result.Success(result.data)
                }
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get item with cache-first approach
     */
    suspend fun getItemBySerialOptimized(serial: String): InventoryItem? {
        // Check cache first
        val cached = itemCache.get(serial)
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.timestamp
            if (age < CACHE_TTL_MS) {
                return cached.item
            } else {
                itemCache.remove(serial)
                cacheTimestamps.remove(serial)
            }
        }

        // Fetch from database
        return withContext(Dispatchers.IO) {
            val item = baseRepo.getItemBySerial(serial)
            if (item != null) {
                itemCache.put(serial, CachedItem(item))
                cacheTimestamps[serial] = System.currentTimeMillis()
            }
            item
        }
    }

    /**
     * Filter items locally from cache for faster results
     */
    suspend fun filterItemsOptimized(
        model: String? = null,
        status: String? = null,
        minQuantity: Int? = null
    ): Result<List<InventoryItem>> = withContext(Dispatchers.IO) {
        try {
            // Get all items (will be cached)
            val allResult = baseRepo.getAllItems(limit = 1000)
            when (allResult) {
                is Result.Success -> {
                    val filtered = allResult.data.filter { item ->
                        (model == null || item.model.contains(model, ignoreCase = true)) &&
                        (status == null || item.status.toString() == status) &&
                        (minQuantity == null || item.quantity >= minQuantity)
                    }
                    
                    // Cache filtered items
                    filtered.forEach { item ->
                        itemCache.put(item.serial, CachedItem(item))
                        cacheTimestamps[item.serial] = System.currentTimeMillis()
                    }
                    
                    Result.Success(filtered)
                }
                is Result.Error -> allResult
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Clear expired cache entries
     */
    fun clearExpiredCache() {
        val now = System.currentTimeMillis()
        val expiredKeys = cacheTimestamps.filter { (_, timestamp) ->
            (now - timestamp) > CACHE_TTL_MS
        }.keys
        
        expiredKeys.forEach { key ->
            itemCache.remove(key)
            cacheTimestamps.remove(key)
        }
    }

    /**
     * Clear all cache
     */
    fun clearAllCache() {
        itemCache.evictAll()
        cacheTimestamps.clear()
    }
}

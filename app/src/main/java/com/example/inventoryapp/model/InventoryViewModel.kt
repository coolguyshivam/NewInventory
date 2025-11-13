package com.example.inventoryapp.model

import androidx.lifecycle.*
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InventoryFilters(
    val serial: String? = null,
    val model: String? = null,
    val quantity: Int? = null,
    val date: String? = null
)

// Make sure InventoryItem has isSold and isInRepair properties.
class InventoryViewModel(
    private val repo: InventoryRepository,
    val userRole: UserRole
) : ViewModel() {

    // Separate lists for AVAILABLE and REPAIR items
    private val _availableInventory = MutableLiveData<List<InventoryItem>>(emptyList())
    val availableInventory: LiveData<List<InventoryItem>> = _availableInventory

    private val _repairInventory = MutableLiveData<List<InventoryItem>>(emptyList())
    val repairInventory: LiveData<List<InventoryItem>> = _repairInventory

    // Legacy: combined inventory for backward compatibility
    private val _inventory = MutableLiveData<List<InventoryItem>>(emptyList())
    val inventory: LiveData<List<InventoryItem>> = _inventory

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _filters = MutableLiveData(InventoryFilters())
    val filters: LiveData<InventoryFilters> = _filters

    private val _transactionHistory = MutableLiveData<List<Transaction>>(emptyList())
    val transactionHistory: LiveData<List<Transaction>> = _transactionHistory

    private val _sortBy = MutableStateFlow("Date")
    val sortBy: StateFlow<String> = _sortBy

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedSerials = MutableStateFlow<Set<String>>(emptySet())
    val selectedSerials: StateFlow<Set<String>> = _selectedSerials

    // Pagination state
    private var lastInventorySerial: String? = null
    private var lastTransactionId: String? = null

    init {
        loadInventory()
    }

    fun loadInventory(paginate: Boolean = false, limit: Int = 100) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.getAllItems(limit = limit, startAfter = if (paginate) lastInventorySerial else null)
            when (result) {
                is Result.Success -> {
                    // Separate items by status
                    val allItems = result.data
                    val available = allItems.filter { it.status == ItemStatus.AVAILABLE && it.quantity > 0 }
                    val repair = allItems.filter { it.status == ItemStatus.REPAIR }
                    
                    // Only delete items with zero quantity that are AVAILABLE, or items that are SOLD/DELETED
                    val itemsToDelete = allItems.filter { 
                        (it.status == ItemStatus.AVAILABLE && it.quantity <= 0) || 
                        it.status == ItemStatus.SOLD ||
                        it.status == ItemStatus.DELETED
                    }
                    itemsToDelete.forEach { item ->
                        repo.deleteItem(item.serial)
                    }
                    
                    if (paginate) {
                        _availableInventory.postValue((_availableInventory.value ?: emptyList()) + available)
                        _repairInventory.postValue((_repairInventory.value ?: emptyList()) + repair)
                        _inventory.postValue((_inventory.value ?: emptyList()) + available + repair)
                    } else {
                        _availableInventory.postValue(available)
                        _repairInventory.postValue(repair)
                        _inventory.postValue(available + repair)
                    }
                    lastInventorySerial = allItems.lastOrNull()?.serial
                }
                is Result.Error -> {
                    _error.postValue(result.exception?.message ?: "Error loading inventory")
                }
            }
            _loading.postValue(false)
        }
    }

    fun loadNextInventoryPage(limit: Int = 100) {
        loadInventory(paginate = true, limit = limit)
    }

    fun searchInventory(query: String) {
        _searchQuery.value = query
        loadInventoryWithFilters()
    }

    fun setFilters(filters: InventoryFilters) {
        _filters.value = filters
        loadInventoryWithFilters()
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
        loadInventoryWithFilters()
    }

    private fun loadInventoryWithFilters() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.getAllItems(limit = 500)
            when (result) {
                is Result.Success -> {
                    // Separate items by status
                    val allItems = result.data
                    val available = allItems.filter { it.status == ItemStatus.AVAILABLE && it.quantity > 0 }
                    val repair = allItems.filter { it.status == ItemStatus.REPAIR }
                    
                    // Only delete items with zero quantity that are AVAILABLE, or items that are SOLD/DELETED
                    val itemsToDelete = allItems.filter { 
                        (it.status == ItemStatus.AVAILABLE && it.quantity <= 0) || 
                        it.status == ItemStatus.SOLD ||
                        it.status == ItemStatus.DELETED
                    }
                    itemsToDelete.forEach { item ->
                        repo.deleteItem(item.serial)
                    }
                    
                    val filters = _filters.value ?: InventoryFilters()
                    val search = _searchQuery.value
                    val sort = _sortBy.value

                    val validItems = available + repair
                    val filtered = validItems.filter { item ->
                        (filters.serial.isNullOrBlank() || item.serial.contains(filters.serial!!, ignoreCase = true)) &&
                        (filters.model.isNullOrBlank() || item.model.contains(filters.model!!, ignoreCase = true)) &&
                        (search.isBlank() ||
                            (item.name.contains(search, true)
                             || item.serial.contains(search, true)
                             || item.model.contains(search, true)
                            )
                        )
                    }
                    val sorted = when (sort) {
                        "Date" -> filtered.sortedByDescending { it.timestamp }
                        "Name" -> filtered.sortedBy { it.name }
                        "Serial" -> filtered.sortedBy { it.serial }
                        else -> filtered
                    }
                    
                    // Update both lists
                    _availableInventory.postValue(sorted.filter { it.status == ItemStatus.AVAILABLE })
                    _repairInventory.postValue(sorted.filter { it.status == ItemStatus.REPAIR })
                    _inventory.postValue(sorted)
                }
                is Result.Error -> {
                    _error.postValue(result.exception?.message ?: "Error loading inventory")
                }
            }
            _loading.postValue(false)
        }
    }

    fun updateInventoryAfterTransaction() {
        loadInventory()
    }

    fun updateSerialFilter(serial: String) {
        _filters.value = _filters.value?.copy(serial = serial)
        loadInventoryWithFilters()
    }

    fun updateModelFilter(model: String) {
        _filters.value = _filters.value?.copy(model = model)
        loadInventoryWithFilters()
    }

    fun loadTransactionHistory(serial: String, paginate: Boolean = false, limit: Int = 20) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.getTransactionsForSerial(serial, limit = limit, startAfter = if (paginate) lastTransactionId else null)
            if (result is Result.Success) {
                if (paginate) {
                    _transactionHistory.postValue((_transactionHistory.value ?: emptyList()) + result.data)
                } else {
                    _transactionHistory.postValue(result.data)
                }
                lastTransactionId = result.data.lastOrNull()?.serial
            } else {
                _transactionHistory.postValue(emptyList())
            }
        }
    }

    fun loadNextTransactionPage(serial: String, limit: Int = 20) {
        loadTransactionHistory(serial, paginate = true, limit = limit)
    }

    // Selection logic for batch actions
    fun selectSerial(serial: String) {
        _selectedSerials.value = _selectedSerials.value + serial
    }
    fun deselectSerial(serial: String) {
        _selectedSerials.value = _selectedSerials.value - serial
    }
    fun toggleSerial(serial: String, selected: Boolean) {
        if (selected) selectSerial(serial) else deselectSerial(serial)
    }
    fun clearSelection() {
        _selectedSerials.value = emptySet()
    }

    // Admin only
    fun editItem(item: InventoryItem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.addOrUpdateItem(item.serial, item)
            onComplete(result is Result.Success)
            if (result is Result.Success) loadInventory()
        }
    }

    fun deleteItem(serial: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.deleteItem(serial)
            onComplete(result is Result.Success)
            if (result is Result.Success) loadInventory()
        }
    }

    fun addTransaction(serial: String, transaction: Transaction, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.addTransaction(serial, transaction)
            onComplete(result is Result.Success)
            if (result is Result.Success) loadInventory()
        }
    }

    fun addBatchTransactions(transactions: List<Transaction>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.addBatchTransactions(transactions)
            onComplete(result is Result.Success)
            if (result is Result.Success) loadInventory()
        }
    }

    fun addBatchInventory(items: List<InventoryItem>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.addBatchInventory(items)
            onComplete(result is Result.Success)
            if (result is Result.Success) loadInventory()
        }
    }

    fun viewHistory(serial: String) {
        loadTransactionHistory(serial)
    }

    fun clearError() {
        _error.value = null
    }

    // Mark item as in repair (AVAILABLE -> REPAIR)
    fun markItemAsRepair(item: InventoryItem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedItem = item.copy(status = ItemStatus.REPAIR)
            val result = repo.addOrUpdateItem(item.serial, updatedItem)
            onComplete(result is Result.Success)
            if (result is Result.Success) loadInventory()
        }
    }

    // Return item from repair (REPAIR -> AVAILABLE)
    fun returnItemFromRepair(item: InventoryItem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedItem = item.copy(status = ItemStatus.AVAILABLE)
            val result = repo.addOrUpdateItem(item.serial, updatedItem)
            onComplete(result is Result.Success)
            if (result is Result.Success) loadInventory()
        }
    }

    companion object {
        fun provideFactory(
            repo: InventoryRepository,
            userRole: UserRole
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return InventoryViewModel(repo, userRole) as T
            }
        }
    }
}
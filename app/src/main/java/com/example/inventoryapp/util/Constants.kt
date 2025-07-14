package com.example.inventoryapp.util

object Constants {
    // Database Configuration
    const val DEFAULT_INVENTORY_LIMIT = 100
    const val DEFAULT_TRANSACTION_LIMIT = 50
    const val DEFAULT_PAGINATION_LIMIT = 20
    const val MAX_MODELS_LIMIT = 1000
    
    // UI Configuration
    const val SPLASH_SCREEN_DELAY = 2000L
    const val AUTO_REFRESH_INTERVAL = 30000L
    const val MAX_IMAGES_PER_ITEM = 5
    const val IMAGE_QUALITY = 85
    
    // Validation Constants
    const val PHONE_NUMBER_LENGTH = 10
    const val AADHAAR_NUMBER_LENGTH = 12
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_DESCRIPTION_LENGTH = 500
    
    // Transaction Types
    const val TRANSACTION_TYPE_SALE = "Sale"
    const val TRANSACTION_TYPE_PURCHASE = "Purchase"
    const val TRANSACTION_TYPE_RETURN = "Return"
    const val TRANSACTION_TYPE_REPAIR = "Repair"
    
    // Date Format
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
    
    // Firebase Collections
    const val COLLECTION_INVENTORY = "inventory"
    const val COLLECTION_TRANSACTIONS = "transactions"
    const val COLLECTION_USERS = "users"
    
    // Permissions
    const val PERMISSION_CAMERA = "android.permission.CAMERA"
    const val PERMISSION_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    const val PERMISSION_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"
    
    // Error Messages
    const val ERROR_NETWORK_UNAVAILABLE = "Network unavailable. Please check your connection."
    const val ERROR_AUTHENTICATION_FAILED = "Authentication failed. Please try again."
    const val ERROR_PERMISSION_DENIED = "Permission denied. Please grant the required permissions."
    const val ERROR_INVALID_INPUT = "Invalid input. Please check your data."
    const val ERROR_ITEM_NOT_FOUND = "Item not found in inventory."
    const val ERROR_TRANSACTION_FAILED = "Transaction failed. Please try again."
    
    // Success Messages
    const val SUCCESS_ITEM_ADDED = "Item added successfully"
    const val SUCCESS_ITEM_UPDATED = "Item updated successfully"
    const val SUCCESS_ITEM_DELETED = "Item deleted successfully"
    const val SUCCESS_TRANSACTION_ADDED = "Transaction added successfully"
    const val SUCCESS_LOGIN = "Login successful"
    const val SUCCESS_LOGOUT = "Logout successful"
}
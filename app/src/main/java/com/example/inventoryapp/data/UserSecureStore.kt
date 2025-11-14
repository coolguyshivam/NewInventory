package com.example.inventoryapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.inventoryapp.model.UserRole
import java.security.MessageDigest

/**
 * Secure storage for user credentials using EncryptedSharedPreferences.
 * Provides methods for CRUD operations on users with encryption at rest.
 */
class UserSecureStore(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Migration flag
    fun isMigrationComplete(): Boolean {
        return securePrefs.getBoolean("migration_complete", false)
    }
    
    fun setMigrationComplete() {
        securePrefs.edit().putBoolean("migration_complete", true).apply()
    }
    
    // User CRUD operations
    fun saveUser(user: User) {
        securePrefs.edit()
            .putString("user_${user.username}_password", user.passwordHash)
            .putString("user_${user.username}_role", user.role.name)
            .apply()
    }
    
    fun getUser(username: String): User? {
        val passwordHash = securePrefs.getString("user_${username}_password", null)
        val roleString = securePrefs.getString("user_${username}_role", null)
        
        return if (passwordHash != null && roleString != null) {
            User(username, passwordHash, UserRole.valueOf(roleString))
        } else null
    }
    
    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        val allKeys = securePrefs.all.keys
        
        // Extract unique usernames from keys like "user_username_password"
        val usernames = allKeys
            .filter { it.startsWith("user_") && it.endsWith("_password") }
            .map { it.removePrefix("user_").removeSuffix("_password") }
            .toSet()
        
        usernames.forEach { username ->
            getUser(username)?.let { users.add(it) }
        }
        
        return users.sortedBy { it.username }
    }
    
    fun deleteUser(username: String) {
        securePrefs.edit()
            .remove("user_${username}_password")
            .remove("user_${username}_role")
            .apply()
    }
    
    fun userExists(username: String): Boolean {
        return securePrefs.contains("user_${username}_password")
    }
    
    fun updateUserRole(username: String, newRole: UserRole): Boolean {
        if (!userExists(username)) return false
        
        securePrefs.edit()
            .putString("user_${username}_role", newRole.name)
            .apply()
        return true
    }
    
    fun updateUserPassword(username: String, newPasswordHash: String): Boolean {
        if (!userExists(username)) return false
        
        securePrefs.edit()
            .putString("user_${username}_password", newPasswordHash)
            .apply()
        return true
    }
    
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

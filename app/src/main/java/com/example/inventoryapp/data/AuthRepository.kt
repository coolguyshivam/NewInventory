package com.example.inventoryapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.inventoryapp.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

data class User(
    val username: String,
    val passwordHash: String,
    val role: UserRole
)

class AuthRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val secureStore = UserSecureStore(context)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Default users - replace with backend source in production
    private val defaultUsers = listOf(
        User("admin", hashPassword("admin123"), UserRole.ADMIN),
        User("operator", hashPassword("operator123"), UserRole.OPERATOR),
        User("viewer", hashPassword("viewer123"), UserRole.VIEWER)
    )

    init {
        // Perform migration from old SharedPreferences to secure store (one-time)
        if (!secureStore.isMigrationComplete()) {
            migrateUsersToSecureStore()
            secureStore.setMigrationComplete()
        }
        
        // Initialize default users if not exist in secure store
        val existingUsers = secureStore.getAllUsers()
        if (existingUsers.isEmpty()) {
            defaultUsers.forEach { user ->
                secureStore.saveUser(user)
            }
        }

        // Check if user is already logged in
        val savedUsername = prefs.getString("current_user", null)
        if (savedUsername != null) {
            _currentUser.value = secureStore.getUser(savedUsername)
        }
    }
    
    /**
     * One-time migration from old SharedPreferences to secure EncryptedSharedPreferences
     */
    private fun migrateUsersToSecureStore() {
        // Check if old users were initialized
        if (prefs.getBoolean("users_initialized", false)) {
            // Migrate existing users from old prefs to secure store
            val allKeys = prefs.all.keys
            val usernames = allKeys
                .filter { it.startsWith("user_") && it.endsWith("_password") }
                .map { it.removePrefix("user_").removeSuffix("_password") }
                .toSet()
            
            usernames.forEach { username ->
                val passwordHash = prefs.getString("user_${username}_password", null)
                val roleString = prefs.getString("user_${username}_role", null)
                
                if (passwordHash != null && roleString != null) {
                    val user = User(username, passwordHash, UserRole.valueOf(roleString))
                    secureStore.saveUser(user)
                }
            }
        }
    }

    fun login(username: String, password: String): Result<User> {
        // Allow blank credentials for testing - default to admin
        if (username.isBlank() && password.isBlank()) {
            val adminUser = secureStore.getAllUsers().firstOrNull { it.role == UserRole.ADMIN }
                ?: defaultUsers.first { it.role == UserRole.ADMIN }
            _currentUser.value = adminUser
            prefs.edit().putString("current_user", adminUser.username).apply()
            return Result.Success(adminUser)
        }

        val user = secureStore.getUser(username)
        return if (user != null && user.passwordHash == hashPassword(password)) {
            _currentUser.value = user
            prefs.edit().putString("current_user", username).apply()
            Result.Success(user)
        } else {
            Result.Error(Exception("Invalid username or password"))
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("current_user").apply()
    }

    fun getCurrentUserRole(): UserRole? = _currentUser.value?.role

    fun isLoggedIn(): Boolean = _currentUser.value != null

    fun canEdit(): Boolean = getCurrentUserRole() in listOf(UserRole.ADMIN, UserRole.OPERATOR)

    fun canDelete(): Boolean = getCurrentUserRole() == UserRole.ADMIN

    fun canViewAnalytics(): Boolean = getCurrentUserRole() == UserRole.ADMIN
    
    fun isAdmin(): Boolean = getCurrentUserRole() == UserRole.ADMIN

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricAvailable()) {
            onError("Biometric authentication not available")
            return
        }

        val lastUsername = prefs.getString("last_biometric_user", null)
        if (lastUsername == null) {
            onError("No biometric user registered")
            return
        }

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val user = secureStore.getUser(lastUsername)
                    if (user != null) {
                        _currentUser.value = user
                        prefs.edit().putString("current_user", lastUsername).apply()
                        onSuccess()
                    } else {
                        onError("User not found")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Use your fingerprint to login")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun enableBiometricForUser(username: String) {
        prefs.edit().putString("last_biometric_user", username).apply()
    }

    // User Management API (ADMIN only operations)
    
    /**
     * Get all users (ADMIN only)
     */
    fun getAllUsers(): List<User> {
        return secureStore.getAllUsers()
    }
    
    /**
     * Add a new user (ADMIN only)
     */
    fun addUser(username: String, password: String, role: UserRole): Result<User> {
        if (!isAdmin()) {
            return Result.Error(Exception("Unauthorized: Admin access required"))
        }
        
        if (username.isBlank()) {
            return Result.Error(Exception("Username cannot be empty"))
        }
        
        if (password.isBlank()) {
            return Result.Error(Exception("Password cannot be empty"))
        }
        
        if (secureStore.userExists(username)) {
            return Result.Error(Exception("User already exists"))
        }
        
        val user = User(username, hashPassword(password), role)
        secureStore.saveUser(user)
        return Result.Success(user)
    }
    
    /**
     * Update user role (ADMIN only)
     */
    fun updateUserRole(username: String, newRole: UserRole): Result<Boolean> {
        if (!isAdmin()) {
            return Result.Error(Exception("Unauthorized: Admin access required"))
        }
        
        if (!secureStore.userExists(username)) {
            return Result.Error(Exception("User not found"))
        }
        
        // Ensure at least one admin exists
        if (newRole != UserRole.ADMIN) {
            val adminCount = secureStore.getAllUsers().count { it.role == UserRole.ADMIN }
            val currentUser = secureStore.getUser(username)
            if (currentUser?.role == UserRole.ADMIN && adminCount <= 1) {
                return Result.Error(Exception("Cannot change role: At least one admin must exist"))
            }
        }
        
        val success = secureStore.updateUserRole(username, newRole)
        return if (success) {
            Result.Success(true)
        } else {
            Result.Error(Exception("Failed to update user role"))
        }
    }
    
    /**
     * Reset user password (ADMIN only)
     */
    fun resetUserPassword(username: String, newPassword: String): Result<Boolean> {
        if (!isAdmin()) {
            return Result.Error(Exception("Unauthorized: Admin access required"))
        }
        
        if (newPassword.isBlank()) {
            return Result.Error(Exception("Password cannot be empty"))
        }
        
        if (!secureStore.userExists(username)) {
            return Result.Error(Exception("User not found"))
        }
        
        val success = secureStore.updateUserPassword(username, hashPassword(newPassword))
        return if (success) {
            Result.Success(true)
        } else {
            Result.Error(Exception("Failed to reset password"))
        }
    }
    
    /**
     * Delete user (ADMIN only)
     */
    fun deleteUser(username: String): Result<Boolean> {
        if (!isAdmin()) {
            return Result.Error(Exception("Unauthorized: Admin access required"))
        }
        
        if (!secureStore.userExists(username)) {
            return Result.Error(Exception("User not found"))
        }
        
        // Cannot delete currently logged in user
        if (_currentUser.value?.username == username) {
            return Result.Error(Exception("Cannot delete currently logged in user"))
        }
        
        // Ensure at least one admin exists
        val user = secureStore.getUser(username)
        if (user?.role == UserRole.ADMIN) {
            val adminCount = secureStore.getAllUsers().count { it.role == UserRole.ADMIN }
            if (adminCount <= 1) {
                return Result.Error(Exception("Cannot delete: At least one admin must exist"))
            }
        }
        
        secureStore.deleteUser(username)
        return Result.Success(true)
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

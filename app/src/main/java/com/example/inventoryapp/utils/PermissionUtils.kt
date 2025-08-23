package com.example.inventoryapp.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Utility class for handling permissions in the NewInventory app
 */
object PermissionUtils {
    
    /**
     * Gets the required permissions for camera functionality
     */
    fun getCameraPermissions(): Array<String> {
        return arrayOf(Manifest.permission.CAMERA)
    }
    
    /**
     * Gets the required permissions for gallery/storage functionality based on Android version
     */
    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Older versions need READ_EXTERNAL_STORAGE
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Gets the required permissions for image downloading based on Android version
     */
    fun getDownloadPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES for reading from gallery
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 can use scoped storage without WRITE_EXTERNAL_STORAGE
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Pre-Android 10 needs both read and write
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    /**
     * Checks if camera permissions are granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return getCameraPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Checks if storage permissions are granted
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return getStoragePermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Checks if download permissions are granted
     */
    fun isDownloadPermissionGranted(context: Context): Boolean {
        return getDownloadPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Creates an intent to open app settings
     */
    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    
    /**
     * Gets user-friendly permission names for display
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.READ_MEDIA_IMAGES -> "Photos and Media"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage"
            else -> "Permission"
        }
    }
    
    /**
     * Gets a user-friendly explanation for why the permission is needed
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera access is needed to take photos for transactions."
            Manifest.permission.READ_MEDIA_IMAGES -> "Photo access is needed to select images from your gallery."
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage access is needed to read images from your device."
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage access is needed to save images to your device."
            else -> "This permission is needed for the app to function properly."
        }
    }
}
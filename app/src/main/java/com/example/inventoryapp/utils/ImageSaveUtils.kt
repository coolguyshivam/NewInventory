package com.example.inventoryapp.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Enhanced image saving utility with proper permissions and user feedback
 */
object ImageSaveUtils {
    
    /**
     * Saves an image from URL to device gallery with proper permission handling
     */
    suspend fun saveImage(
        context: Context,
        url: String,
        filename: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Check permissions first
                val permissionResult = checkAndGetPermissionError(context)
                if (permissionResult != null) {
                    onError(permissionResult)
                    return@withContext
                }
                
                // Download image
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    onError("Failed to download image: HTTP ${connection.responseCode}")
                    return@withContext
                }
                
                val inputStream = connection.inputStream
                
                // Save to MediaStore
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    
                    // Use scoped storage approach for Android 10+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                
                if (imageUri == null) {
                    onError("Failed to create image entry in gallery")
                    return@withContext
                }
                
                try {
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        inputStream.use { inp ->
                            inp.copyTo(outputStream)
                        }
                    } ?: throw IOException("Failed to open output stream")
                    
                    // Mark as not pending for Android 10+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val updateValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        resolver.update(imageUri, updateValues, null, null)
                    }
                    
                    onSuccess("Image saved to gallery successfully!")
                    
                } catch (e: Exception) {
                    // Clean up on failure
                    try {
                        resolver.delete(imageUri, null, null)
                    } catch (deleteException: Exception) {
                        // Ignore cleanup errors
                    }
                    onError("Failed to save image: ${e.message}")
                }
                
            } catch (e: Exception) {
                onError("Download failed: ${e.message}")
            }
        }
    }
    
    /**
     * Checks if required permissions are granted and returns error message if not
     */
    private fun checkAndGetPermissionError(context: Context): String? {
        return when {
            // Android 13+ (API 33+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val permission = Manifest.permission.READ_MEDIA_IMAGES
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    "Permission required: Please grant media access to save images"
                } else null
            }
            
            // Android 10-12 (API 29-32) - Scoped storage, no permission needed for MediaStore
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                null // No permission needed for MediaStore on these versions
            }
            
            // Android 9 and below (API 28-) - Need WRITE_EXTERNAL_STORAGE
            else -> {
                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    "Permission required: Please grant storage access to save images"
                } else null
            }
        }
    }
    
    /**
     * Returns the required permission for the current Android version
     */
    fun getRequiredPermission(): String? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_IMAGES
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> null // No permission needed
            else -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }
    
    /**
     * Checks if permission is granted for current Android version
     */
    fun hasRequiredPermission(context: Context): Boolean {
        val permission = getRequiredPermission() ?: return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
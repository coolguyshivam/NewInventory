package com.example.inventoryapp.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.inventoryapp.utils.ImageUtils

class ImagePickerHandler(
    val context: Context,
    val maxImages: Int,
    val onGalleryDenied: (String) -> Unit,
    val onCameraDenied: (String) -> Unit,
    val onImagesSelected: (List<Uri>) -> Unit,
    val onImageCaptured: (Uri) -> Unit
) {
    private var galleryLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>? = null
    private var setCameraImageUri: ((Uri) -> Unit)? = null
    
    fun setupLaunchers(
        galleryLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
        cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
        setCameraImageUri: (Uri) -> Unit
    ) {
        this.galleryLauncher = galleryLauncher
        this.cameraLauncher = cameraLauncher
        this.setCameraImageUri = setCameraImageUri
    }
    
    fun launchGallery() {
        galleryLauncher?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            ?: onGalleryDenied("Gallery launcher not initialized")
    }

    fun launchCamera() {
        try {
            val uri = ImageUtils.createCameraImageUri(context)
            setCameraImageUri?.invoke(uri)
            cameraLauncher?.launch(uri) ?: onCameraDenied("Camera launcher not initialized")
        } catch (e: Exception) {
            onCameraDenied("Failed to launch camera: ${e.message}")
        }
    }
}
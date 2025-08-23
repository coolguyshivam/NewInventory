package com.example.inventoryapp.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.inventoryapp.utils.ImageUtils
import com.example.inventoryapp.utils.PermissionUtils

class ImagePickerHandler(
    val context: Context,
    val maxImages: Int,
    val onGalleryDenied: (String) -> Unit,
    val onCameraDenied: (String) -> Unit,
    val onImagesSelected: (List<Uri>) -> Unit,
    val onImageCaptured: (Uri) -> Unit,
    val onPermissionNeeded: (Array<String>, String) -> Unit // New callback for permission requests
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
        if (PermissionUtils.isStoragePermissionGranted(context)) {
            galleryLauncher?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                ?: onGalleryDenied("Gallery launcher not initialized")
        } else {
            onPermissionNeeded(
                PermissionUtils.getStoragePermissions(),
                "Storage permission is needed to select images from your gallery."
            )
        }
    }

    fun launchCamera() {
        if (PermissionUtils.isCameraPermissionGranted(context)) {
            try {
                val uri = ImageUtils.createCameraImageUri(context)
                setCameraImageUri?.invoke(uri)
                cameraLauncher?.launch(uri) ?: onCameraDenied("Camera launcher not initialized")
            } catch (e: Exception) {
                onCameraDenied("Failed to launch camera: ${e.message}")
            }
        } else {
            onPermissionNeeded(
                PermissionUtils.getCameraPermissions(),
                "Camera permission is needed to take photos."
            )
        }
    }
}
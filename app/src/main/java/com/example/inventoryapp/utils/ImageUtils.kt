package com.example.inventoryapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Creates a temporary file Uri for camera image capture.
     */
    fun createCameraImageUri(context: Context): Uri {
        val imagesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "transaction_photo_${System.currentTimeMillis()}",
            ".jpg",
            imagesDir
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    /**
     * Compresses the image if needed, returns a Uri to the compressed file.
     * If the image is already small (width/height < 1080), it returns the original Uri.
     *
     * @param context Android context
     * @param uri Uri of the original image
     * @return Uri of the compressed image (may be the same as input)
     */
    fun compressImageIfNeeded(context: Context, uri: Uri): Uri {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                // Use BitmapFactory for lower APIs instead of deprecated MediaStore.Images.Media.getBitmap
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = if (inputStream != null) {
                    android.graphics.BitmapFactory.decodeStream(inputStream)
                } else {
                    null
                }
                inputStream?.close()
                bmp ?: return uri
            }
            val maxDim = 1080
            val scale = minOf(
                maxDim / bitmap.width.toFloat(),
                maxDim / bitmap.height.toFloat(),
                1f
            )
            val outBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }
            val tempFile = File.createTempFile("compressed_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out ->
                outBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            return Uri.fromFile(tempFile)
        } catch (e: Exception) {
            // If anything fails, just return the original Uri
            return uri
        }
    }
}
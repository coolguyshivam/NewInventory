package com.example.inventoryapp.ui.screens

import android.annotation.SuppressLint
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun BarcodeScannerScreen(
    navController: NavController,
    onBarcodeScanned: (String) -> Unit = { scannedCode ->
        // Default: set result and go back
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("scannedSerial", scannedCode)
        navController.popBackStack()
    }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (scanning) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val scanner: BarcodeScanner = BarcodeScanning.getClient()
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx)
                            ) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val code = barcodes.firstOrNull { it.rawValue != null }
                                            if (code != null) {
                                                scanning = false
                                                onBarcodeScanned(code.rawValue ?: "")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            errorMsg = e.localizedMessage
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        errorMsg = e.localizedMessage
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator()
        }
        errorMsg?.let { Text("Error: $it") }
    }
}
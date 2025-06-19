package com.example.inventoryapp.ui.screens

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BarcodeScannerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    var scannedCode by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                fun bindCamera() {
                    startCamera(
                        context = ctx,
                        previewView = previewView,
                        lifecycleOwner = lifecycleOwner,
                        cameraExecutor = cameraExecutor
                    ) { barcode ->
                        if (scannedCode == null && barcode.all { it.isDigit() }) {
                            scannedCode = barcode
                            coroutineScope.launch(Dispatchers.Main) {
                                // Save to previous entry’s savedStateHandle and pop
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("scannedSerial", barcode)
                                navController.popBackStack()
                            }
                        }
                    }
                }

                bindCamera()
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (scannedCode == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

private fun startCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraExecutor: java.util.concurrent.Executor,
    onBarcodeScanned: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy, onBarcodeScanned)
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
            Log.e("CameraX", "Camera bind failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    onBarcodeScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // Use a regular for loop, not forEach!
                for (barcode in barcodes) {
                    val value = barcode.rawValue ?: continue
                    if (value.all { it.isDigit() }) {
                        onBarcodeScanned(value)
                        break
                    }
                }
            }
            .addOnFailureListener { /* Handle error if desired */ }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
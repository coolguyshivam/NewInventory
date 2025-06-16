package com.example.inventoryapp.ui.screens

import android.Manifest
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionRequested) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            permissionRequested = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            hasCameraPermission -> {
                CameraPreview(onBarcodeScanned = onBarcodeScanned)
            }
            showPermissionRationale -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission is required for scanning barcodes.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Grant Permission")
                    }
                }
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun CameraPreview(onBarcodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var lastScanned by remember { mutableStateOf("") }
    var lastScanTime by remember { mutableStateOf(0L) }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(lifecycleOwner) {
        val mainHandler = Handler(Looper.getMainLooper())
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor, object : ImageAnalysis.Analyzer {
                            override fun analyze(imageProxy: ImageProxy) {
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    val scanner = BarcodeScanning.getClient()
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val now = System.currentTimeMillis()
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                if (!rawValue.isNullOrBlank()
                                                    && rawValue != lastScanned
                                                    && now - lastScanTime > 2000
                                                ) {
                                                    lastScanned = rawValue
                                                    lastScanTime = now
                                                    mainHandler.post {
                                                        Toast.makeText(context, "Scanned: $rawValue", Toast.LENGTH_SHORT).show()
                                                    }
                                                    onBarcodeScanned(rawValue)
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("BarcodeScanner", "Error scanning barcode", e)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                cameraError = e.localizedMessage
                Log.e("BarcodeScanner", "Camera binding failed", e)
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProviderFuture.get().unbindAll()
            cameraExecutor.shutdown()
        }
    }

    cameraError?.let {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}
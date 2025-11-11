package com.example.inventoryapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Real barcode scanner screen using CameraX and ML Kit
 * Focuses on IMEI scanning with Luhn algorithm validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeReaderScreen(
    navController: NavController,
    onBarcodeScanned: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Permission state — initialize as false and check in LaunchedEffect to avoid surprising behavior
    var hasCameraPermission by remember { mutableStateOf(false) }
    var showRetryButton by remember { mutableStateOf(false) }
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isValidIMEI by remember { mutableStateOf(false) }
    var continuousMode by remember { mutableStateOf(false) }

    // Background executor for analysis
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Launcher for permission request
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        showRetryButton = !isGranted
    }

    // Safe initial permission check
    LaunchedEffect(key1 = context) {
        hasCameraPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            // Try to request permission (this will show system dialog)
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Shutdown executor when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) { }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Scanner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !hasCameraPermission -> {
                    // Permission denied / request UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Camera permission is required to scan barcodes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Camera Permission")
                        }
                        if (showRetryButton) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                // Open app settings for manual permission granting (covers permanent denial)
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }) {
                                Text("Open Settings")
                            }
                        }
                    }
                }

                scannedCode != null -> {
                    // Show scanned result and actions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Scanned Code", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            scannedCode ?: "",
                            modifier = Modifier.testTag("imeiValue"),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                // copy to clipboard
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(scannedCode ?: ""))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Copied to clipboard")
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Copy")
                            }

                            Button(onClick = {
                                // Return the scanned code to caller and previous nav entry
                                scannedCode?.let { code ->
                                    try {
                                        onBarcodeScanned?.invoke(code)
                                        navController.previousBackStackEntry?.savedStateHandle?.set("scannedSerial", code)
                                    } catch (_: Exception) { }
                                }
                                navController.popBackStack()
                            }) {
                                Text("Use Code")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                scannedCode = null
                            }) {
                                Text("Scan Another")
                            }

                            OutlinedButton(onClick = {
                                continuousMode = !continuousMode
                                scannedCode = null
                            }) {
                                Text(if (continuousMode) "Single Mode" else "Continuous Mode")
                            }
                        }
                    }
                }

                else -> {
                    // Camera preview with scanner
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("barcodeScanner")
                    ) {
                        BarcodeCamera(
                            modifier = Modifier.fillMaxSize(),
                            lifecycleOwner = lifecycleOwner,
                            cameraExecutor = cameraExecutor,
                            onBarcodeDetected = { code ->
                                // Debounce and validate
                                if (scannedCode == null) {
                                    scannedCode = code
                                    isValidIMEI = isValidIMEI(code)
                                    // In continuousMode, auto clear non-IMEI scans
                                    if (continuousMode && !isValidIMEI) {
                                        scope.launch(Dispatchers.Default) {
                                            kotlinx.coroutines.delay(2000)
                                            scannedCode = null
                                        }
                                    }
                                }
                            }
                        )

                        // Overlay instructions
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                "Point camera at barcode or QR code",
                                modifier = Modifier.padding(12.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Mode indicator
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (continuousMode)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                else
                                    Color.Black.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                if (continuousMode) "Continuous Scanning" else "Single Scan Mode",
                                modifier = Modifier.padding(8.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable hosting the camera preview and the MLKit analyzer.
 */
@Composable
private fun BarcodeCamera(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraController = LifecycleCameraController(ctx)

            // Configure barcode scanner options
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val barcodeScanner = BarcodeScanning.getClient(options)

            try {
                cameraController.setImageAnalysisAnalyzer(
                    cameraExecutor,
                    MlKitAnalyzer(
                        listOf(barcodeScanner),
                        CameraController.COORDINATE_SYSTEM_ORIGINAL,
                        cameraExecutor
                    ) { result ->
                        try {
                            val barcodeResults = result.getValue(barcodeScanner)
                            if (barcodeResults != null && barcodeResults.isNotEmpty()) {
                                barcodeResults.firstOrNull()?.rawValue?.let { value ->
                                    onBarcodeDetected(value)
                                }
                            }
                        } catch (_: Exception) {
                            // swallow analysis exceptions — prevents crashes from malformed frames
                        }
                    }
                )

                // Bind lifecycle safely — lifecycleOwner comes from parent composable
                cameraController.bindToLifecycle(lifecycleOwner)
                previewView.controller = cameraController
            } catch (ex: Exception) {
                // If binding fails, swallow and allow UI to show fallback
            }

            previewView
        },
        update = { /* no-op */ }
    )
}

/**
 * Basic IMEI validation (Luhn check for 15-digit IMEI).
 */
private fun isValidIMEI(code: String?): Boolean {
    if (code == null) return false
    val digits = code.filter { it.isDigit() }
    if (digits.length != 15) return false

    // Luhn algorithm
    var sum = 0
    for (i in digits.indices) {
        var d = digits[digits.length - 1 - i].digitToInt()
        if (i % 2 == 1) {
            d *= 2
            if (d > 9) d -= 9
        }
        sum += d
    }
    return sum % 10 == 0
}
package com.example.inventoryapp.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
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
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isValidIMEI by remember { mutableStateOf(false) }
    var showRetryButton by remember { mutableStateOf(false) }
    var continuousMode by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        showRetryButton = !isGranted
    }
    
    // Initialize camera permission request
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Scanner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    // Permission denied state
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
                
                scannedCode != null -> {
                    // Show scanned result
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isValidIMEI) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (isValidIMEI) "Valid IMEI Detected!" else "Barcode Scanned",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValidIMEI) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    scannedCode!!,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isValidIMEI) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.testTag("imeiValue")
                                )
                                if (!isValidIMEI && scannedCode!!.length == 15) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Invalid IMEI checksum",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Copy to clipboard
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("IMEI", scannedCode)
                                    clipboard.setPrimaryClip(clip)
                                    
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Copied to clipboard!")
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy")
                            }
                            
                            Button(
                                onClick = {
                                    // Return the scanned code
                                    onBarcodeScanned?.invoke(scannedCode!!)
                                    navController.previousBackStackEntry?.savedStateHandle?.set("scannedSerial", scannedCode)
                                    navController.popBackStack()
                                }
                            ) {
                                Text("Use Code")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scannedCode = null
                                    // Continue scanning
                                }
                            ) {
                                Text("Scan Another")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    continuousMode = !continuousMode
                                    scannedCode = null
                                }
                            ) {
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
                            onBarcodeDetected = { code ->
                                if (scannedCode == null) { // Prevent multiple detections
                                    scannedCode = code
                                    isValidIMEI = isValidIMEI(code)
                                    
                                    // Auto-continue if in continuous mode and not IMEI
                                    if (continuousMode && !isValidIMEI) {
                                        scope.launch {
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

@Composable
private fun BarcodeCamera(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraController = LifecycleCameraController(ctx)
            
            // Configure barcode scanner
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_ALL_FORMATS
                )
                .build()
            
            val barcodeScanner = BarcodeScanning.getClient(options)
            
            cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(ctx),
                MlKitAnalyzer(
                    listOf(barcodeScanner),
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(ctx)
                ) { result ->
                    val barcodeResults = result.getValue(barcodeScanner)
                    if (barcodeResults != null && barcodeResults.isNotEmpty()) {
                        barcodeResults.firstOrNull()?.rawValue?.let { value ->
                            onBarcodeDetected(value)
                        }
                    }
                }
            )
            
            cameraController.bindToLifecycle(lifecycleOwner)
            previewView.controller = cameraController
            previewView
        },
        modifier = modifier
    )
}

/**
 * Validates IMEI using Luhn algorithm (modulo 10)
 */
private fun isValidIMEI(imei: String): Boolean {
    if (imei.length != 15 || !imei.all { it.isDigit() }) {
        return false
    }
    
    var sum = 0
    var shouldDouble = false
    
    // Process digits from right to left (excluding check digit)
    for (i in imei.length - 2 downTo 0) {
        var digit = imei[i].digitToInt()
        
        if (shouldDouble) {
            digit *= 2
            if (digit > 9) {
                digit = digit / 10 + digit % 10
            }
        }
        
        sum += digit
        shouldDouble = !shouldDouble
    }
    
    val checkDigit = (10 - (sum % 10)) % 10
    return checkDigit == imei.last().digitToInt()
}
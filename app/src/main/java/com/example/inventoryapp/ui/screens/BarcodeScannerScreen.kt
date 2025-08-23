package com.example.inventoryapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.InventoryViewModel
import com.example.inventoryapp.utils.IMEIValidator
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    navController: NavController,
    viewModel: InventoryViewModel,
    inventoryRepo: InventoryRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var scannedText by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            showPermissionDialog = true
        }
    }
    
    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Process scan result
    LaunchedEffect(scannedText) {
        scannedText?.let { text ->
            isScanning = false
            
            if (IMEIValidator.isIMEIFormat(text)) {
                if (IMEIValidator.isValidIMEI(text)) {
                    // Check if IMEI exists in inventory
                    val existingItem = inventoryRepo.getItemBySerial(text)
                    scanResult = if (existingItem != null) {
                        ScanResult.ItemFound(existingItem)
                    } else {
                        ScanResult.CreateNew(text)
                    }
                } else {
                    scanResult = ScanResult.InvalidIMEI(text)
                }
            } else {
                scanResult = ScanResult.NotIMEI(text)
            }
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                // Permission denied UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To scan barcodes, please grant camera permission.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text("Grant Permission")
                    }
                }
            } else if (scanResult != null) {
                // Show scan result
                ScanResultContent(
                    result = scanResult!!,
                    onRescan = {
                        scannedText = null
                        scanResult = null
                        isScanning = true
                    },
                    onNavigateToItem = { item ->
                        // Navigate to item details or edit screen
                        navController.navigate("transaction_screen?type=Edit&serial=${item.serial}&model=${item.model}")
                    },
                    onCreateNewItem = { imei ->
                        // Navigate to create new item with IMEI pre-filled
                        navController.navigate("transaction_screen?type=Purchase&serial=$imei&model=")
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Camera preview
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        CameraPreview(
                            onBarcodeScanned = { barcode ->
                                scannedText = barcode
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Scanning overlay
                        ScanningOverlay(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Instructions
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Point camera at IMEI barcode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Looking for 15-digit IMEI numbers",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data class for scan results
sealed class ScanResult {
    data class ItemFound(val item: InventoryItem) : ScanResult()
    data class CreateNew(val imei: String) : ScanResult()
    data class InvalidIMEI(val scannedText: String) : ScanResult()
    data class NotIMEI(val scannedText: String) : ScanResult()
}

@Composable
fun ScanResultContent(
    result: ScanResult,
    onRescan: () -> Unit,
    onNavigateToItem: (InventoryItem) -> Unit,
    onCreateNewItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (result) {
            is ScanResult.ItemFound -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Item Found!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Serial: ${result.item.serial}")
                        Text("Name: ${result.item.name}")
                        Text("Model: ${result.item.model}")
                        if (result.item.description.isNotBlank()) {
                            Text("Description: ${result.item.description}")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onRescan) {
                        Text("Scan Again")
                    }
                    Button(
                        onClick = { onNavigateToItem(result.item) }
                    ) {
                        Text("View Item")
                    }
                }
            }
            
            is ScanResult.CreateNew -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "New IMEI Detected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("IMEI: ${result.imei}")
                        Text("This IMEI is not in your inventory.")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onRescan) {
                        Text("Scan Again")
                    }
                    Button(
                        onClick = { onCreateNewItem(result.imei) }
                    ) {
                        Text("Add New Item")
                    }
                }
            }
            
            is ScanResult.InvalidIMEI -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Invalid IMEI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Scanned: ${result.scannedText}")
                        Text("This appears to be a 15-digit number but fails IMEI validation.")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRescan) {
                    Text("Scan Again")
                }
            }
            
            is ScanResult.NotIMEI -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Orange,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Not an IMEI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Orange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Scanned: ${result.scannedText}")
                        Text("Please scan a 15-digit IMEI barcode.")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRescan) {
                    Text("Scan Again")
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, BarcodeAnalyzer(onBarcodeScanned))
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    // Handle camera binding exception
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun ScanningOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Scanning frame
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(
                    Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .padding(4.dp)
        ) {
            // Corner indicators
            repeat(4) { index ->
                val alignment = when (index) {
                    0 -> Alignment.TopStart
                    1 -> Alignment.TopEnd
                    2 -> Alignment.BottomStart
                    else -> Alignment.BottomEnd
                }
                
                Box(
                    modifier = Modifier
                        .align(alignment)
                        .size(24.dp)
                        .background(
                            Color.White,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

// Barcode analyzer for MLKit
class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val scanner = BarcodeScanning.getClient()
    private var lastScannedTime = 0L
    private val scanCooldown = 2000L // 2 seconds cooldown
    
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScannedTime < scanCooldown) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            lastScannedTime = currentTime
                            onBarcodeScanned(value)
                            break
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
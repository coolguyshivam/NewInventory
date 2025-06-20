package com.example.inventoryapp.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.ui.components.InventoryCard
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.swiperefresh.SwipeRefresh
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }
    var expandedNames by remember { mutableStateOf(setOf<String>()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Pull-to-refresh logic
    fun refreshInventory() {
        isRefreshing = true
        scope.launch {
            kotlinx.coroutines.delay(400)
            isRefreshing = false
        }
    }

    // --- Robust local inventory collection ---
    // If you do NOT use Flow, just call a suspend function in LaunchedEffect:
    val inventory = remember { mutableStateListOf<InventoryItem>() }

    // Simulate loading inventory items
    LaunchedEffect(isRefreshing) {
        inventory.clear()
        val list = inventoryRepo.getAllInventoryItems() // <-- Implement this in your InventoryRepository
        inventory.addAll(list)
    }

    // Barcode scan integration
    val scannedSerial = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("scannedSerial")
    LaunchedEffect(scannedSerial) {
        if (!scannedSerial.isNullOrBlank()) {
            filterText = scannedSerial
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scannedSerial")
        }
    }

    // Filter logic
    val filteredInventory = remember(inventory, filterText) {
        if (filterText.isBlank()) inventory.toList()
        else inventory.filter {
            (it.name ?: "").contains(filterText, ignoreCase = true)
                || (it.model ?: "").contains(filterText, ignoreCase = true)
                || (it.serial ?: "").contains(filterText, ignoreCase = true)
        }
    }
    val groupedInventory = remember(filteredInventory) {
        filteredInventory.groupBy { it.name ?: "(No Name)" }
    }

    // Image picker and take photo logic
    val cameraPermission = Manifest.permission.CAMERA
    val readImagePermission = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasReadPermission by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasReadPermission = granted }
    )

    // Pick image from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    // Take photo with camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) capturedImageUri = capturedImageUri
        }
    )

    fun createImageFile(): File {
        val fileName = "IMG_${UUID.randomUUID()}.jpg"
        val storageDir = context.getExternalFilesDir("Pictures")
        return File(storageDir, fileName)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter/search bar
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                label = { Text("Filter by Name/Model/Serial") },
                singleLine = true,
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = { filterText = "" }) {
                            Icon(Icons.Default.Image, contentDescription = "Clear")
                        }
                    }
                }
            )

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { refreshInventory() },
                modifier = Modifier.weight(1f)
            ) {
                if (inventory.isEmpty() && !isRefreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No items in inventory.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedInventory.forEach { (name, itemsForName) ->
                            item(key = name) {
                                var expanded by remember { mutableStateOf(expandedNames.contains(name)) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clickable {
                                            expanded = !expanded
                                            expandedNames = if (expanded) expandedNames + name else expandedNames - name
                                        }
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Qty: ${itemsForName.sumOf { it.quantity }}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                        }
                                        if (expanded) {
                                            Divider()
                                            itemsForName.forEach { item ->
                                                InventoryCard(
                                                    item = item,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    onSell = {
                                                        navController.navigate(
                                                            "transaction_screen?type=Sale&serial=${item.serial}&model=${item.model}"
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Show selected or captured image (for demo)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                selectedImageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = "Picked Image",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                capturedImageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = "Captured Image",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // FABs (barcode scan, pick image, take photo)
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = { navController.navigate("barcode_scanner") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode", tint = Color.White)
            }
            FloatingActionButton(
                onClick = {
                    if (!hasReadPermission) readPermissionLauncher.launch(readImagePermission)
                    else photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Image, contentDescription = "Pick Image", tint = Color.White)
            }
            FloatingActionButton(
                onClick = {
                    if (!hasCameraPermission) cameraPermissionLauncher.launch(cameraPermission)
                    else {
                        val file = createImageFile()
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        capturedImageUri = uri
                        takePictureLauncher.launch(uri)
                    }
                },
                containerColor = Color(0xFF4CAF50)
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Take Photo", tint = Color.White)
            }
        }
    }
}